package com.isaac.souqalghiyar.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.isaac.souqalghiyar.domain.model.Order
import com.isaac.souqalghiyar.domain.model.OrderItem
import com.isaac.souqalghiyar.domain.model.OrderWithItems
import com.isaac.souqalghiyar.domain.model.admin_alarm
import com.isaac.souqalghiyar.domain.repository.OrderRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : OrderRepository {

    override suspend fun submitOrderWithItems(order: Order, items: List<OrderItem>): Result<Unit> {
        return try {
            // جلب رقم هاتف المستخدم من جدول users قبل البدء بعملية الحفظ
            val userSnapshot = db.collection("users").document(order.user_id).get().await()
            val userPhoneNumber = userSnapshot.getString("phone_number") ?: "غير متوفر"

            val counterRef = db.collection("counters").document("orders")
            val orderRef = db.collection("orders").document()

            db.runTransaction { transaction ->
                val snapshot = transaction.get(counterRef)
                val currentNumber = if (snapshot.exists()) snapshot.getLong("last_number") ?: 0L else 0L
                val newOrderNumber = currentNumber + 1

                val counterData = hashMapOf<String, Any>("last_number" to newOrderNumber)
                transaction.set(counterRef, counterData, SetOptions.merge())

                // دمج رقم الهاتف (user_number) داخل كائن الطلب قبل حفظه في القاعدة
                val finalOrder = order.copy(
                    order_id = orderRef.id,
                    order_number = newOrderNumber,
                    user_number = userPhoneNumber
                )
                transaction.set(orderRef, finalOrder)

                items.forEach { item ->
                    val itemRef = orderRef.collection("items").document()
                    val finalItem = item.copy(item_id = itemRef.id)
                    transaction.set(itemRef, finalItem)
                }

                // إنشاء إشعار واحد فقط للإدارة
                val adminAlarmRef = db.collection("admin_alarm").document()
                val adminAlarmData = admin_alarm(
                    alarm_id = adminAlarmRef.id,
                    date = com.google.firebase.Timestamp.now(),
                    order_number = newOrderNumber,
                    title = "طلب تسعيرة جديد",
                    message = "قام العميل بطلب فاتورة عرض سعر جديدة برقم $newOrderNumber",
                    fcm_token = "", // السيرفر سيرسله للمجموعة المشتركة تلقائياً
                    isRead = false
                )
                transaction.set(adminAlarmRef, adminAlarmData)

                true
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override fun getCategories(): Flow<List<String>> = callbackFlow {
        val sub = db.collection("spare_parts_categories").addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.getString("spare_parts_categories") }
                trySend(list).isSuccess
            }
        }
        awaitClose { sub.remove() }
    }

    override fun getQualityTypes(): Flow<List<String>> = callbackFlow {
        val sub = db.collection("quality_types").addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.getString("quality_types") }
                trySend(list).isSuccess
            }
        }
        awaitClose { sub.remove() }
    }

    override fun getLocations(): Flow<List<String>> = callbackFlow {
        val sub = db.collection("location").addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.getString("location") }
                trySend(list).isSuccess
            }
        }
        awaitClose { sub.remove() }
    }

    override suspend fun incrementUserRejections(userId: String): Result<Unit> {
        return try {
            db.collection("users").document(userId).update("number_of_rejections", FieldValue.increment(1.0)).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override fun getUserOrders(userId: String): Flow<List<OrderWithItems>> = callbackFlow {
        // الاستعلام مبني على الـ user_id حصراً
        val subscription = db.collection("orders").whereEqualTo("user_id", userId).addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            if (snapshot == null || snapshot.isEmpty) { trySend(emptyList()); return@addSnapshotListener }

            launch {
                try {
                    val orderList = mutableListOf<OrderWithItems>()
                    for (doc in snapshot.documents) {
                        val order = doc.toObject(Order::class.java)?.copy(order_id = doc.id)
                        if (order != null) {
                            val itemsSnapshot = db.collection("orders").document(order.order_id).collection("items").get().await()
                            val items = itemsSnapshot.documents.mapNotNull { itemDoc ->
                                itemDoc.toObject(OrderItem::class.java)?.copy(item_id = itemDoc.id)
                            }
                            orderList.add(OrderWithItems(order, items))
                        }
                    }
                    send(orderList.sortedByDescending { it.order.created_at })
                } catch (e: Exception) {
                    e.printStackTrace()
                    send(emptyList())
                }
            }
        }
        awaitClose { subscription.remove() }
    }

    override suspend fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        approvalNotes: String,
        disapprovalNotes: String
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "order_status" to newStatus,
                "order_status_date" to com.google.firebase.Timestamp.now(),
                "approval_notes" to approvalNotes,
                "disapproval_notes" to disapprovalNotes
            )
            db.collection("orders").document(orderId).update(updates).await()

            val orderSnapshot = db.collection("orders").document(orderId).get().await()
            val orderNumber = orderSnapshot.getLong("order_number") ?: 0L

            // مسح إشعارات المستخدم القديمة لهذا الطلب
            val userAlarms = db.collection("user_alarm").whereEqualTo("order_number", orderNumber).get().await()
            for (doc in userAlarms.documents) {
                db.collection("user_alarm").document(doc.id).delete().await()
            }

            // 🌟 التعديل هنا: إشعار واحد فقط مخصص للإدارة 🌟
            if (newStatus == "going" || newStatus == "canceled") {
                val title = if (newStatus == "going") "طلب بانتظار التوصيل 🚚" else "تم رفض التسعيرة ❌"
                val message = if (newStatus == "going") "تمت موافقة العميل على الفاتورة للطلب رقم $orderNumber، بانتظار التوصيل." else "قام العميل برفض التسعيرة للطلب رقم $orderNumber."

                val adminAlarmRef = db.collection("admin_alarm").document()
                val adminAlarmData = admin_alarm(
                    alarm_id = adminAlarmRef.id,
                    date = com.google.firebase.Timestamp.now(),
                    order_number = orderNumber,
                    title = title,
                    message = message,
                    fcm_token = "",
                    isRead = false
                )
                db.collection("admin_alarm").document(adminAlarmRef.id).set(adminAlarmData).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

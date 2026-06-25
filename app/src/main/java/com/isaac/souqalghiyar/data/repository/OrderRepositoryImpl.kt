package com.isaac.souqalghiyar.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.isaac.souqalghiyar.domain.model.Order
import com.isaac.souqalghiyar.domain.model.OrderItem
import com.isaac.souqalghiyar.domain.model.OrderWithItems
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
            val counterRef = db.collection("counters").document("orders")
            val orderRef = db.collection("orders").document()

            db.runTransaction { transaction ->
                val snapshot = transaction.get(counterRef)
                val currentNumber = if (snapshot.exists()) snapshot.getLong("last_number") ?: 0L else 0L
                val newOrderNumber = currentNumber + 1

                val counterData = hashMapOf<String, Any>("last_number" to newOrderNumber)
                transaction.set(counterRef, counterData, SetOptions.merge())

                val finalOrder = order.copy(
                    order_id = orderRef.id,
                    order_number = newOrderNumber
                )
                transaction.set(orderRef, finalOrder)

                items.forEach { item ->
                    val itemRef = orderRef.collection("items").document()
                    val finalItem = item.copy(item_id = itemRef.id)
                    transaction.set(itemRef, finalItem)
                }
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
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.getString("spare_parts_categories") }
                trySend(list).isSuccess
            }
        }
        awaitClose { sub.remove() }
    }

    override fun getQualityTypes(): Flow<List<String>> = callbackFlow {
        val sub = db.collection("quality_types").addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.getString("quality_types") }
                trySend(list).isSuccess
            }
        }
        awaitClose { sub.remove() }
    }

    override fun getLocations(): Flow<List<String>> = callbackFlow {
        val sub = db.collection("location").addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.getString("location") }
                trySend(list).isSuccess
            }
        }
        awaitClose { sub.remove() }
    }

    override fun getUserOrders(userId: String): Flow<List<OrderWithItems>> = callbackFlow {
        val subscription = db.collection("orders")
            .whereEqualTo("user_id", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // استخدمنا launch المدمج مع callbackFlow لضمان دورة الحياة الصحيحة للمسار
                launch {
                    try {
                        val orderList = mutableListOf<OrderWithItems>()

                        for (doc in snapshot.documents) {
                            val order = doc.toObject(Order::class.java)?.copy(order_id = doc.id)
                            if (order != null) {
                                // جلب القطع الفرعية بأمان
                                val itemsSnapshot = db.collection("orders").document(order.order_id).collection("items").get().await()
                                val items = itemsSnapshot.documents.mapNotNull { itemDoc ->
                                    itemDoc.toObject(OrderItem::class.java)?.copy(item_id = itemDoc.id)
                                }
                                orderList.add(OrderWithItems(order, items))
                            }
                        }

                        // إرسال البيانات المجمعة مرتبة من الأحدث للأقدم
                        send(orderList.sortedByDescending { it.order.created_at })
                        
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // التعديل الأهم: إرسال قائمة فارغة في حال حدوث أي خطأ لمنع واجهة الطلبات من التعليق في وضع التحميل
                        send(emptyList())
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    // التعديل هنا لرفع حالة الطلب مع ملاحظات الموافقة والرفض
    override suspend fun updateOrderStatus(
        orderId: String, 
        newStatus: String,
        approvalNotes: String,
        disapprovalNotes: String
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "order_status" to newStatus,
                "approval_notes" to approvalNotes,
                "disapproval_notes" to disapprovalNotes
            )
            db.collection("orders").document(orderId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

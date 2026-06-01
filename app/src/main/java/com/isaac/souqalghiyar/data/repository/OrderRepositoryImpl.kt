package com.isaac.souqalghiyar.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.isaac.souqalghiyar.domain.model.Order
import com.isaac.souqalghiyar.domain.model.OrderItem
import com.isaac.souqalghiyar.domain.model.OrderWithItems
import com.isaac.souqalghiyar.domain.repository.OrderRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : OrderRepository {

    // 1. إرسال الطلب مع قائمة القطع (استخدام Transaction لضمان الأمان والتسلسل)
    override suspend fun submitOrderWithItems(order: Order, items: List<OrderItem>): Result<Unit> {
        return try {
            val counterRef = db.collection("counters").document("orders")
            val orderRef = db.collection("orders").document() // إنشاء Auto-ID للطلب

            db.runTransaction { transaction ->
                // أ. قراءة العداد الحالي للطلبات
                val snapshot = transaction.get(counterRef)
                val currentNumber = if (snapshot.exists()) snapshot.getLong("last_number") ?: 0L else 0L
                val newOrderNumber = currentNumber + 1

                // ب. تحديث العداد في قاعدة البيانات
                transaction.update(counterRef, "last_number", newOrderNumber)

                // ج. تجهيز الطلب الرئيسي بالرقم المتسلسل الجديد والمعرف
                val finalOrder = order.copy(
                    order_id = orderRef.id,
                    order_number = newOrderNumber
                )
                transaction.set(orderRef, finalOrder)

                // د. حفظ جميع القطع المضافة داخل الـ Subcollection الخاص بهذا الطلب
                items.forEach { item ->
                    val itemRef = orderRef.collection("items").document() // Auto-ID للقطعة
                    val finalItem = item.copy(item_id = itemRef.id)
                    transaction.set(itemRef, finalItem)
                }

                null // الـ Transaction تتطلب إرجاع قيمة
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // 2. جلب أقسام القطع للقائمة المنسدلة
    override fun getCategories(): Flow<List<String>> = flowOf(
        listOf("ذراع أمامي", "مقص", "فحمات", "فلتر زيت", "بواجي", "صدام")
    )

    // 3. جلب أنواع الجودة للقائمة المنسدلة
    override fun getQualityTypes(): Flow<List<String>> = flowOf(
        listOf("وكالة", "درجة أولى", "درجة ثانية", "تجاري صيني")
    )

    // 4. جلب طلبات المستخدم مع القطع التابعة لها لعرضها في شاشة الطلبات
    override fun getUserOrders(userId: String): Flow<List<OrderWithItems>> = callbackFlow {
        val subscription = db.collection("orders")
            .whereEqualTo("user_id", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val orderList = mutableListOf<OrderWithItems>()
                    
                    if (snapshot.isEmpty) {
                        trySend(emptyList()).isSuccess
                        return@addSnapshotListener
                    }

                    // جلب البيانات لكل طلب والبحث عن قطعه
                    snapshot.documents.forEach { doc ->
                        val order = doc.toObject(Order::class.java)?.copy(order_id = doc.id)
                        if (order != null) {
                            db.collection("orders").document(order.order_id)
                                .collection("items")
                                .get()
                                .addOnSuccessListener { itemsSnapshot ->
                                    val items = itemsSnapshot.documents.mapNotNull { itemDoc -> 
                                        itemDoc.toObject(OrderItem::class.java)?.copy(item_id = itemDoc.id) 
                                    }
                                    orderList.add(OrderWithItems(order, items))
                                    // إرسال البيانات مرتبة من الأحدث للأقدم
                                    trySend(orderList.sortedByDescending { it.order.created_at }).isSuccess
                                }
                        }
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    // 5. تحديث حالة الطلب (عند موافقة العميل أو إلغائه)
    override suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Unit> {
        return try {
            db.collection("orders").document(orderId).update("order_status", newStatus).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

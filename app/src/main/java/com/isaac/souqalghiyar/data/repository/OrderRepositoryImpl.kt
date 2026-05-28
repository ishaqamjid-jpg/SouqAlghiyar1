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

    override suspend fun submitOrderWithItem(order: Order, item: OrderItem): Result<Unit> {
        return try {
            // مراجع الوثائق
            val counterRef = db.collection("counters").document("orders")
            val orderRef = db.collection("orders").document()
            val itemRef = orderRef.collection("items").document()

            // استخدام Transaction لضمان التسلسل وعدم التداخل
            db.runTransaction { transaction ->
                // 1. قراءة العداد الحالي
                val snapshot = transaction.get(counterRef)
                val currentNumber = if (snapshot.exists()) snapshot.getLong("last_number") ?: 0L else 0L
                val newOrderNumber = currentNumber + 1

                // 2. تحديث العداد في قاعدة البيانات
                transaction.update(counterRef, "last_number", newOrderNumber)

                // 3. تجهيز الطلب بالرقم الجديد والـ ID
                val finalOrder = order.copy(
                    order_id = orderRef.id,
                    order_number = newOrderNumber
                )
                transaction.set(orderRef, finalOrder)

                // 4. تجهيز القطعة وحفظها
                val finalItem = item.copy(item_id = itemRef.id)
                transaction.set(itemRef, finalItem)
                
                null // Transaction تطلب إرجاع قيمة، نعيد null للنجاح
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override fun getCategories(): Flow<List<String>> = flowOf(
        listOf("ذراع أمامي", "مقص", "فحمات", "فلتر زيت", "بواجي", "صدام")
    )

    override fun getQualityTypes(): Flow<List<String>> = flowOf(
        listOf("وكالة", "درجة أولى", "درجة ثانية", "تجاري صيني")
    )

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
                    
                    snapshot.documents.forEach { doc ->
                        val order = doc.toObject(Order::class.java)?.copy(order_id = doc.id)
                        if (order != null) {
                            db.collection("orders").document(order.order_id)
                                .collection("items")
                                .get()
                                .addOnSuccessListener { itemsSnapshot ->
                                    val items = itemsSnapshot.documents.mapNotNull { it.toObject(OrderItem::class.java) }
                                    orderList.add(OrderWithItems(order, items))
                                    trySend(orderList.sortedByDescending { it.order.created_at }).isSuccess
                                }
                        }
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Unit> {
        return try {
            db.collection("orders").document(orderId).update("order_status", newStatus).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

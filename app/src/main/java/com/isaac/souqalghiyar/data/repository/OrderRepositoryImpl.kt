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
            val batch = db.batch()
            val orderRef = db.collection("orders").document()
            val finalOrder = order.copy(order_id = orderRef.id)
            batch.set(orderRef, finalOrder)

            val itemRef = orderRef.collection("items").document()
            val finalItem = item.copy(item_id = itemRef.id)
            batch.set(itemRef, finalItem)

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCategories(): Flow<List<String>> = flowOf(
        listOf("ذراع أمامي", "مقص", "فحمات", "فلتر زيت", "بواجي", "صدام")
    )

    override fun getQualityTypes(): Flow<List<String>> = flowOf(
        listOf("وكالة", "درجة أولى", "درجة ثانية", "تجاري صيني")
    )

    // تنفيذ جلب الطلبات للمستخدم العادي (مزامنة حية)
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
                    // ملاحظة: جلب الـ Subcollections يفضل أن يكون عبر دالة معلقة داخل الكوروتين،
                    // لكن لغرض الـ SnapshotListener سنقوم بجلبها بشكل متزامن.
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

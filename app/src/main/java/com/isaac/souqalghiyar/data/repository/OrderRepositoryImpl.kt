package com.isaac.souqalghiyar.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.isaac.souqalghiyar.domain.model.Order
import com.isaac.souqalghiyar.domain.model.OrderItem
import com.isaac.souqalghiyar.domain.model.OrderWithItems
import com.isaac.souqalghiyar.domain.repository.OrderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

                transaction.update(counterRef, "last_number", newOrderNumber)

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
                if (snapshot != null) {
                    val orderList = mutableListOf<OrderWithItems>()
                    if (snapshot.isEmpty) {
                        trySend(emptyList()).isSuccess
                        return@addSnapshotListener
                    }

                    // استخدام CoroutineScope مع الاستيرادات الصحيحة في الأعلى
                    CoroutineScope(Dispatchers.IO).launch {
                        snapshot.documents.forEach { doc ->
                            val order = doc.toObject(Order::class.java)?.copy(order_id = doc.id)
                            if (order != null) {
                                try {
                                    val itemsSnapshot = db.collection("orders").document(order.order_id).collection("items").get().await()
                                    val items = itemsSnapshot.documents.mapNotNull { itemDoc ->
                                        itemDoc.toObject(OrderItem::class.java)?.copy(item_id = itemDoc.id)
                                    }
                                    orderList.add(OrderWithItems(order, items))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        trySend(orderList.sortedByDescending { it.order.created_at }).isSuccess
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
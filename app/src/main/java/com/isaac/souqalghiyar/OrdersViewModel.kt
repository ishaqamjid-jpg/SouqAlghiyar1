package com.isaac.souqalghiyar.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.isaac.souqalghiyar.domain.model.Order
import com.isaac.souqalghiyar.domain.model.OrderItem
import com.isaac.souqalghiyar.domain.model.OrderWithItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _orders = MutableStateFlow<List<OrderWithItems>>(emptyList())
    val orders: StateFlow<List<OrderWithItems>> = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // جلب طلبات المستخدم المعلقة والسابقة
    fun fetchUserOrders(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. جلب الطلبات الأساسية
                val snapshot = db.collection("orders")
                    .whereEqualTo("user_id", userId)
                    .get()
                    .await()

                val orderList = mutableListOf<OrderWithItems>()

                for (doc in snapshot.documents) {
                    val order = doc.toObject(Order::class.java)?.copy(order_id = doc.id)
                    if (order != null) {
                        // 2. جلب القطع الفرعية (Subcollection) لكل طلب
                        val itemsSnapshot = db.collection("orders").document(order.order_id).collection("items").get().await()
                        val items = itemsSnapshot.documents.mapNotNull { it.toObject(OrderItem::class.java) }
                        
                        orderList.add(OrderWithItems(order, items))
                    }
                }
                
                // ترتيب الطلبات (الأحدث أولاً)
                _orders.value = orderList.sortedByDescending { it.order.created_at }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // دالة موافقة العميل أو رفضه للتسعيرة
    fun updateOrderStatus(orderId: String, newStatus: String, userId: String) {
        viewModelScope.launch {
            try {
                db.collection("orders").document(orderId).update("order_status", newStatus).await()
                fetchUserOrders(userId) // تحديث القائمة بعد التغيير
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

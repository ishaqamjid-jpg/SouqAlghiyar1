package com.isaac.souqalghiyar.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isaac.souqalghiyar.domain.model.OrderWithItems
import com.isaac.souqalghiyar.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _orders = MutableStateFlow<List<OrderWithItems>>(emptyList())
    val orders: StateFlow<List<OrderWithItems>> = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var fetchJob: Job? = null

    fun fetchUserOrders(userId: String) {
        if (userId.isBlank()) {
            _isLoading.value = false
            return
        }

        fetchJob?.cancel() 
        fetchJob = viewModelScope.launch {
            _isLoading.value = true
            orderRepository.getUserOrders(userId)
                .catch { e ->
                    e.printStackTrace()
                    _isLoading.value = false
                }
                .collect { orderList ->
                    _orders.value = orderList
                    _isLoading.value = false
                }
        }
    }

    // تم إضافة userId هنا لمعرفة صاحب الطلب
    fun updateStatus(orderId: String, userId: String, newStatus: String, approvalNotes: String = "", disapprovalNotes: String = "") {
        viewModelScope.launch {
            // تحديث حالة الطلب
            orderRepository.updateOrderStatus(orderId, newStatus, approvalNotes, disapprovalNotes)
            
            // إذا قام العميل برفض الفاتورة، نزيد عداد الرفض في حسابه
            if (newStatus == "canceled") {
                orderRepository.incrementUserRejections(userId)
            }
        }
    }
}

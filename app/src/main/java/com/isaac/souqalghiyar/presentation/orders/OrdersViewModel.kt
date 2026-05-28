package com.isaac.souqalghiyar.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isaac.souqalghiyar.domain.model.OrderWithItems
import com.isaac.souqalghiyar.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository // حقن المستودع النظيف
) : ViewModel() {

    private val _orders = MutableStateFlow<List<OrderWithItems>>(emptyList())
    val orders: StateFlow<List<OrderWithItems>> = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun fetchUserOrders(userId: String) {
        _isLoading.value = true
        viewModelScope.launch {
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

    fun updateOrderStatus(orderId: String, newStatus: String, userId: String) {
        viewModelScope.launch {
            orderRepository.updateOrderStatus(orderId, newStatus)
            // لا نحتاج لاستدعاء fetchUserOrders لأن الـ Flow سيحدث الشاشة تلقائياً
        }
    }
}

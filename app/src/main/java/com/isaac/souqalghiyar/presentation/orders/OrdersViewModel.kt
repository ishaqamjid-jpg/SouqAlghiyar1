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

    // قائمة الطلبات (تحتوي على الطلب + القطع التابعة له)
    private val _orders = MutableStateFlow<List<OrderWithItems>>(emptyList())
    val orders: StateFlow<List<OrderWithItems>> = _orders.asStateFlow()

    // حالة التحميل (لإظهار مؤشر التحميل في الواجهة)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * جلب طلبات المستخدم المعلقة والسابقة
     * نستخدم Flow (مزامنة حية) بحيث لو تغيرت حالة الطلب من الإدارة
     * تتحدث شاشة العميل تلقائياً وبدون تحديث يدوي.
     */
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

    /**
     * دالة تحديث حالة الطلب
     * تُستدعى عندما يضغط العميل على (موافقة واعتماد) أو (إلغاء الطلب)
     */
    fun updateOrderStatus(orderId: String, newStatus: String, userId: String) {
        viewModelScope.launch {
            orderRepository.updateOrderStatus(orderId, newStatus)
            // ملاحظة: لا نحتاج لاستدعاء fetchUserOrders مرة أخرى هنا
            // لأن الـ SnapshotListener (الـ Flow) في الـ Repository سيكتشف التغيير ويحدث الشاشة فوراً!
        }
    }
}
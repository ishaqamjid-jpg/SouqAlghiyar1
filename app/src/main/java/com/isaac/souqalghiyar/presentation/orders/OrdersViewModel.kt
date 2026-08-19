package com.isaac.souqalghiyar.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.isaac.souqalghiyar.domain.model.OrderWithItems
import com.isaac.souqalghiyar.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

    // تقوم بمسح الإشعارات الخاصة بالطلبات المكتملة عند دخول المستخدم لقسم الطلبات السابقة
    fun clearCompletedOrderAlarms(userId: String, orders: List<OrderWithItems>) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                // نأخذ فقط أرقام الطلبات المكتملة
                val completedOrderNumbers = orders
                    .filter { it.order.order_status.trim().lowercase() == "completed" }
                    .map { it.order.order_number }

                if (completedOrderNumbers.isEmpty()) return@launch

                // البحث عن الإشعارات التي تخص هذا المستخدم
                val userAlarms = db.collection("user_alarm")
                    .whereEqualTo("receiver_id", userId)
                    .get().await()

                // المرور على الإشعارات ومسح ما يطابق أرقام الطلبات المكتملة
                for (doc in userAlarms.documents) {
                    val alarmOrderNumber = doc.getLong("order_number")
                    if (alarmOrderNumber != null && completedOrderNumbers.contains(alarmOrderNumber)) {
                        doc.reference.delete().await()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateStatus(orderId: String, userId: String, orderNumber: Long, newStatus: String, approvalNotes: String = "", disapprovalNotes: String = "") {
        viewModelScope.launch {
            try {
                // 1. تحديث حالة الطلب
                orderRepository.updateOrderStatus(orderId, newStatus, approvalNotes, disapprovalNotes)

                // 2. إذا قام العميل برفض الفاتورة، نزيد عداد الرفض في حسابه
                if (newStatus == "canceled") {
                    orderRepository.incrementUserRejections(userId)
                }

                val db = FirebaseFirestore.getInstance()

                // 3. مسح إشعار المستخدم (المتعلق بطلب الموافقة) بعد اتخاذه للقرار
                val userAlarms = db.collection("user_alarm").whereEqualTo("order_number", orderNumber).get().await()
                for (doc in userAlarms.documents) {
                    doc.reference.delete().await()
                }

                // 4. إرسال إشعار للإدارة (في تطبيق الداش بورد) بناءً على رد العميل
                val adminAlarmRef = db.collection("admin_alarm").document()

                if (newStatus == "going") {
                    adminAlarmRef.set(hashMapOf(
                        "alarm_id" to adminAlarmRef.id,
                        "date" to com.google.firebase.Timestamp.now(),
                        "order_number" to orderNumber,
                        "title" to "طلب بانتظار التوصيل \uD83D\uDE9A",
                        "message" to "تمت موافقة العميل على الفاتورة للطلب رقم $orderNumber، بانتظار التوصيل.",
                        "isRead" to false
                    )).await()
                } else if (newStatus == "canceled") {
                    adminAlarmRef.set(hashMapOf(
                        "alarm_id" to adminAlarmRef.id,
                        "date" to com.google.firebase.Timestamp.now(),
                        "order_number" to orderNumber,
                        "title" to "تم رفض التسعيرة \u274C",
                        "message" to "قام العميل برفض التسعيرة للطلب رقم $orderNumber.",
                        "isRead" to false
                    )).await()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
package com.isaac.souqalghiyar.domain.repository

import com.isaac.souqalghiyar.domain.model.Order
import com.isaac.souqalghiyar.domain.model.OrderItem
import com.isaac.souqalghiyar.domain.model.OrderWithItems
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    // 1. تم التعديل هنا لتستقبل قائمة من القطع (List<OrderItem>) بصيغة الجمع
    suspend fun submitOrderWithItems(order: Order, items: List<OrderItem>): Result<Unit>

    // 2. جلب أقسام القطع
    fun getCategories(): Flow<List<String>>

    // 3. جلب أنواع الجودة
    fun getQualityTypes(): Flow<List<String>>

    // 4. جلب طلبات المستخدم
    fun getUserOrders(userId: String): Flow<List<OrderWithItems>>

    // 5. تحديث حالة الطلب
    suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Unit>
}
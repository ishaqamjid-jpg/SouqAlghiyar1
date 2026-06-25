package com.isaac.souqalghiyar.domain.repository

import com.isaac.souqalghiyar.domain.model.Order
import com.isaac.souqalghiyar.domain.model.OrderItem
import com.isaac.souqalghiyar.domain.model.OrderWithItems
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    suspend fun submitOrderWithItems(order: Order, items: List<OrderItem>): Result<Unit>
    fun getCategories(): Flow<List<String>>
    fun getQualityTypes(): Flow<List<String>>
    fun getLocations(): Flow<List<String>>
    fun getUserOrders(userId: String): Flow<List<OrderWithItems>>
    
    // التعديل هنا: إضافة حقول الملاحظات كقيمة افتراضية فارغة
    suspend fun updateOrderStatus(
        orderId: String, 
        newStatus: String, 
        approvalNotes: String = "", 
        disapprovalNotes: String = ""
    ): Result<Unit>
}

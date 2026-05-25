package com.isaac.souqalghiyar.domain.repository

import com.isaac.souqalghiyar.domain.model.Order
import com.isaac.souqalghiyar.domain.model.OrderItem
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    // إرسال الطلب وحفظه
    suspend fun submitOrderWithItem(order: Order, item: OrderItem): Result<Unit>
    // جلب القوائم المنسدلة (يمكنك لاحقاً ربطها بـ Firestore، حالياً سنضعها كقيم ثابتة للتجربة)
    fun getCategories(): Flow<List<String>>
    fun getQualityTypes(): Flow<List<String>>
}

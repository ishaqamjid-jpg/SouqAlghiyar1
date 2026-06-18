package com.isaac.souqalghiyar.domain.repository

import com.isaac.souqalghiyar.domain.model.Advertisement
import kotlinx.coroutines.flow.Flow

interface MainRepository {
    fun getActiveAdvertisements(): Flow<List<Advertisement>>
    fun getBrands(): Flow<List<String>> // لجلب الماركات
    
    // الدالة الجديدة: للتحقق من وجود طلبات معلقة بانتظار الموافقة
    fun hasWaitingForApprovalOrders(userId: String): Flow<Boolean>
}

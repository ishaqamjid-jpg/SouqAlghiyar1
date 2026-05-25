package com.isaac.souqalghiyar.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.isaac.souqalghiyar.domain.model.Order
import com.isaac.souqalghiyar.domain.model.OrderItem
import com.isaac.souqalghiyar.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : OrderRepository {

    override suspend fun submitOrderWithItem(order: Order, item: OrderItem): Result<Unit> {
        return try {
            val batch = db.batch()

            // 1. إنشاء مرجع للطلب الرئيسي (Auto-ID)
            val orderRef = db.collection("orders").document()
            val finalOrder = order.copy(order_id = orderRef.id)
            batch.set(orderRef, finalOrder)

            // 2. إنشاء مرجع للقطعة كـ Subcollection داخل الطلب
            val itemRef = orderRef.collection("items").document()
            val finalItem = item.copy(item_id = itemRef.id)
            batch.set(itemRef, finalItem)

            // 3. تنفيذ العمليتين معاً
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // يمكن لاحقاً جلبها من Firestore، هنا قيم افتراضية لعمل الواجهة
    override fun getCategories(): Flow<List<String>> = flowOf(
        listOf("ذراع أمامي", "مقص", "فحمات", "فلتر زيت", "بواجي", "صدام")
    )

    override fun getQualityTypes(): Flow<List<String>> = flowOf(
        listOf("وكالة", "درجة أولى", "درجة ثانية", "تجاري صيني")
    )
}

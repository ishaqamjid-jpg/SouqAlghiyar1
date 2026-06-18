package com.isaac.souqalghiyar.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.isaac.souqalghiyar.domain.model.Advertisement
import com.isaac.souqalghiyar.domain.repository.MainRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class MainRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : MainRepository {

    override fun getActiveAdvertisements(): Flow<List<Advertisement>> = callbackFlow {
        val subscription = firestore.collection("advertisements")
            .whereEqualTo("is_active", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // جلب الإعلانات مباشرة وترتيبها برمجياً حسب الأولوية
                    val ads = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Advertisement::class.java)
                    }.sortedByDescending { it.priority }

                    trySend(ads).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getBrands(): Flow<List<String>> = callbackFlow {
        val subscription = firestore.collection("brands")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val brands = snapshot.documents.mapNotNull { it.getString("brand_name") }
                    trySend(brands).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }

    // التنفيذ الفعلي للدالة الجديدة
    override fun hasWaitingForApprovalOrders(userId: String): Flow<Boolean> = callbackFlow {
        // البحث في مجموعة الطلبات
        val subscription = firestore.collection("orders")
            .whereEqualTo("user_id", userId) // مطابقة المستخدم الحالي
            .whereEqualTo("order_status", "waiting for approval") // مطابقة حالة الطلب
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // إذا لم يكن الـ snapshot فارغاً، فهذا يعني أن هناك طلبات معلقة (نرجع true)
                    // إذا كان فارغاً نرجع false
                    val hasPending = !snapshot.isEmpty
                    trySend(hasPending).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }
}

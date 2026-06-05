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
                    val ads = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Advertisement::class.java)?.copy(ad_id = doc.id)
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
}

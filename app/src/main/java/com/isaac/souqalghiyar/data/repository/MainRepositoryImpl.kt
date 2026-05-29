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

    override suspend fun getAdvertisements(): Flow<List<Advertisement>> = callbackFlow {
        val subscription = firestore.collection("advertisements")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val ads = snapshot.documents.mapNotNull { doc ->
                        Advertisement(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            content = doc.getString("content") ?: ""
                        )
                    }
                    trySend(ads).isSuccess
                }
            }

        awaitClose { subscription.remove() }
    }
}
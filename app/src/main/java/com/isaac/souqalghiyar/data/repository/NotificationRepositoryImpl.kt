package com.isaac.souqalghiyar.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.isaac.souqalghiyar.domain.model.user_alarm
import com.isaac.souqalghiyar.domain.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : NotificationRepository {
    override fun getUserNotifications(userId: String): Flow<List<user_alarm>> = callbackFlow {
        val sub = db.collection("user_alarm")
            .whereEqualTo("receiver_id", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val alarms = snapshot.documents.mapNotNull { it.toObject(user_alarm::class.java)?.copy(alarm_id = it.id) }
                    trySend(alarms.sortedByDescending { it.date }).isSuccess
                }
            }
        awaitClose { sub.remove() }
    }

    override suspend fun markNotificationAsRead(alarmId: String): Result<Unit> {
        return try {
            db.collection("user_alarm").document(alarmId).update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNotification(alarmId: String): Result<Unit> {
        return try {
            db.collection("user_alarm").document(alarmId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

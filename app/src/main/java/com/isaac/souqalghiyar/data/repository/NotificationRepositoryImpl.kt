package com.isaac.souqalghiyar.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.isaac.souqalghiyar.domain.model.PublicAdvertisement
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

    override fun getUserAlarms(userId: String): Flow<List<user_alarm>> = callbackFlow {
        val sub = db.collection("user_alarm")
            .whereEqualTo("receiver_id", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val alarms = mutableListOf<user_alarm>()
                    val thirtyDaysInMs = 30L * 24 * 60 * 60 * 1000 // 30 يوماً
                    val currentTime = System.currentTimeMillis()

                    for (doc in snapshot.documents) {
                        val alarm = doc.toObject(user_alarm::class.java)?.copy(alarm_id = doc.id)
                        if (alarm != null) {
                            val alarmTime = alarm.date?.toDate()?.time ?: currentTime
                            // الميزة: مسح الإشعارات القديمة تلقائياً من القاعدة لتوفير المساحة
                            if (currentTime - alarmTime > thirtyDaysInMs) {
                                doc.reference.delete() 
                            } else {
                                alarms.add(alarm)
                            }
                        }
                    }
                    trySend(alarms.sortedByDescending { it.date }).isSuccess
                }
            }
        awaitClose { sub.remove() }
    }

    override fun getPublicAds(): Flow<List<PublicAdvertisement>> = callbackFlow {
        val sub = db.collection("public_advertisements")
            .whereEqualTo("category", "all")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val publicAds = mutableListOf<PublicAdvertisement>()
                    val thirtyDaysInMs = 30L * 24 * 60 * 60 * 1000 
                    val currentTime = System.currentTimeMillis()

                    for (doc in snapshot.documents) {
                        val ad = doc.toObject(PublicAdvertisement::class.java)?.copy(doc_id = doc.id)
                        if (ad != null) {
                            val adTime = ad.create_date?.toDate()?.time ?: currentTime
                            val endTime = ad.end_date?.toDate()?.time
                            val isExpired = endTime != null && currentTime > endTime

                            // إخفاء الإعلانات المنتهية أو القديمة أكثر من شهر (لا يتم مسحها من هنا لأنها للكل)
                            if (currentTime - adTime <= thirtyDaysInMs && !isExpired) {
                                publicAds.add(ad)
                            }
                        }
                    }
                    trySend(publicAds).isSuccess
                }
            }
        awaitClose { sub.remove() }
    }

    override suspend fun markNotificationAsRead(alarmId: String): Result<Unit> {
        return try {
            db.collection("user_alarm").document(alarmId).update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteNotification(alarmId: String): Result<Unit> {
        return try {
            db.collection("user_alarm").document(alarmId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}

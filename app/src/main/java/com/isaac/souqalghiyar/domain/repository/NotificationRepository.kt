package com.isaac.souqalghiyar.domain.repository

import com.isaac.souqalghiyar.domain.model.PublicAdvertisement
import com.isaac.souqalghiyar.domain.model.user_alarm
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getUserAlarms(userId: String): Flow<List<user_alarm>>
    fun getPublicAds(): Flow<List<PublicAdvertisement>>
    suspend fun markNotificationAsRead(alarmId: String): Result<Unit>
    suspend fun deleteNotification(alarmId: String): Result<Unit>
}

package com.isaac.souqalghiyar.domain.repository

import com.isaac.souqalghiyar.domain.model.user_alarm
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getUserNotifications(userId: String): Flow<List<user_alarm>>
    suspend fun markNotificationAsRead(alarmId: String): Result<Unit>
    suspend fun deleteNotification(alarmId: String): Result<Unit>
}

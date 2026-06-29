package com.isaac.souqalghiyar.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isaac.souqalghiyar.domain.model.user_alarm
import com.isaac.souqalghiyar.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<user_alarm>>(emptyList())
    val notifications: StateFlow<List<user_alarm>> = _notifications.asStateFlow()

    fun fetchNotifications(userId: String) {
        viewModelScope.launch {
            repository.getUserNotifications(userId).collect { alarms ->
                _notifications.value = alarms
            }
        }
    }

    fun markAsRead(alarmId: String) {
        viewModelScope.launch { repository.markNotificationAsRead(alarmId) }
    }

    fun deleteNotification(alarmId: String) {
        viewModelScope.launch { repository.deleteNotification(alarmId) }
    }
}


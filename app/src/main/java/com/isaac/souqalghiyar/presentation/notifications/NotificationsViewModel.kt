package com.isaac.souqalghiyar.presentation.notifications

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.isaac.souqalghiyar.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// موديل موحد لعرض الإشعارات معاً بطريقة أنيقة
data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val date: Timestamp?,
    val isRead: Boolean,
    val type: String // "order", "advertisement", "public_ad"
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val sharedPreferences: SharedPreferences 
) : ViewModel() {

    private val _deletedPublicAds = MutableStateFlow<Set<String>>(
        sharedPreferences.getStringSet("deleted_public_ads", emptySet()) ?: emptySet()
    )
    private val _readPublicAds = MutableStateFlow<Set<String>>(
        sharedPreferences.getStringSet("read_public_ads", emptySet()) ?: emptySet()
    )

    private val _userId = MutableStateFlow("")

    // دمج الإشعارات الخاصة والعامة معاً
    val notifications: StateFlow<List<NotificationItem>> = combine(
        _userId.filter { it.isNotEmpty() }.flatMapLatest { repository.getUserAlarms(it) },
        repository.getPublicAds(),
        _deletedPublicAds,
        _readPublicAds
    ) { userAlarms, publicAds, deletedAds, readAds ->
        
        val items = mutableListOf<NotificationItem>()

        // الإشعارات الخاصة (طلبات أو إعلانات مخصصة)
        userAlarms.forEach { alarm ->
            items.add(
                NotificationItem(
                    id = alarm.alarm_id,
                    title = alarm.title,
                    message = alarm.message,
                    date = alarm.date,
                    isRead = alarm.isRead,
                    type = alarm.type
                )
            )
        }

        // الإشعارات العامة (مع التأكد من عدم إظهار المحذوفة يدوياً)
        publicAds.forEach { pubAd ->
            if (!deletedAds.contains(pubAd.doc_id)) {
                items.add(
                    NotificationItem(
                        id = pubAd.doc_id,
                        title = pubAd.title,
                        message = pubAd.message,
                        date = pubAd.create_date,
                        isRead = readAds.contains(pubAd.doc_id),
                        type = "public_ad"
                    )
                )
            }
        }

        // الترتيب من الأحدث للأقدم
        items.sortedByDescending { it.date?.seconds ?: 0 }
        
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun fetchNotifications(userId: String) {
        _userId.value = userId
    }

    fun markAsRead(id: String, type: String) {
        if (type == "public_ad") {
            val newRead = _readPublicAds.value.toMutableSet().apply { add(id) }
            _readPublicAds.value = newRead
            sharedPreferences.edit().putStringSet("read_public_ads", newRead).apply()
        } else {
            viewModelScope.launch { repository.markNotificationAsRead(id) }
        }
    }

    fun deleteNotification(id: String, type: String) {
        if (type == "public_ad") {
            val newDeleted = _deletedPublicAds.value.toMutableSet().apply { add(id) }
            _deletedPublicAds.value = newDeleted
            sharedPreferences.edit().putStringSet("deleted_public_ads", newDeleted).apply()
        } else {
            viewModelScope.launch { repository.deleteNotification(id) }
        }
    }
}

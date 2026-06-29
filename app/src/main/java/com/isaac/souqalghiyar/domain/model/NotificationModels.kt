package com.isaac.souqalghiyar.domain.model

import com.google.firebase.Timestamp

data class user_alarm(
    val alarm_id: String = "",
    val date: Timestamp? = null,
    val order_number: Long = 0L,
    val message: String = "",
    val title: String = "",
    val receiver_id: String = "",
    val fcm_token: String = "",
    val isRead: Boolean = false
)

data class admin_alarm(
    val alarm_id: String = "",
    val date: Timestamp? = null,
    val order_number: Long = 0L,
    val message: String = "",
    val title: String = "",
    val isRead: Boolean = false
)

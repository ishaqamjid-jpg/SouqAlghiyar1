package com.isaac.souqalghiyar.domain.model

import com.google.firebase.Timestamp

data class users(
    val user_id: String = "",
    val phone_number: String = "",
    val display_name: String = "",
    val fcm_token: String = "",
    val status: String = "active",
     val number_of_rejections: 
    val created_at: Timestamp? = null
)

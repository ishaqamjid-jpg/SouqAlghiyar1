package com.isaac.souqalghiyar.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName


data class Advertisement(
    val ad_id: String = "",
    val name: String = "",
    val title: String = "",
    val image_url: String = "",
    val click_action_type: String = "",
    val target_url: String? = null,
    val start_date: Timestamp? = null,
    val end_date: Timestamp? = null,
    val priority: Int = 0,
    val is_active: Boolean = true,
    val created_at: Timestamp? = null
)
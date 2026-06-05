package com.isaac.souqalghiyar.domain.model

import com.google.firebase.Timestamp

data class Advertisement(
    val title: String = "",
    val image_url: String = "",
    val click_action_type: String = "",
    val target_url: String? = null,
    val start_date: Timestamp? = null,
    val end_date: Timestamp? = null,
    val priority: Int = 0, // هذا هو المتغير المطلوب لترتيب الإعلانات
    val is_active: Boolean = true,
    val created_at: Timestamp? = null
)
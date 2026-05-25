package com.isaac.souqalghiyar.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Order(
    val order_id: String = "",
    val user_id: String = "",
    val vehicle_name: String = "",
    val vehicle_model: String = "",
    val pic_vin_number: String = "",
    val delivery_location: String = "",
    val delivery_fees: Double = 0.0,
    val order_status: String = "pending",
    @ServerTimestamp val created_at: Date? = null
)

data class OrderItem(
    val item_id: String = "",
    val part_name: String = "",
    val quantity: Int = 1,
    val quality_type: String = "",
    val description: String = "",
    val comments: String = "",
    val purchase_price: Double = 0.0,
    val selling_price: Double = 0.0
)

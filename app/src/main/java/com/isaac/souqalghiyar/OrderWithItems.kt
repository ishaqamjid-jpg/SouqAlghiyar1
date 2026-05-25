package com.isaac.souqalghiyar.domain.model

// كلاس يجمع الطلب مع قطعه لعرضها في الشاشة بسهولة
data class OrderWithItems(
    val order: Order,
    val items: List<OrderItem>
)

package com.isaac.souqalghiyar.presentation.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.isaac.souqalghiyar.domain.model.OrderWithItems
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    userId: String,
    viewModel: OrdersViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("الطلبات المعلقة", "الطلبات السابقة")

    LaunchedEffect(userId) {
        viewModel.fetchUserOrders(userId)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("طلباتي", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1B6D), titleContentColor = Color.White)
                )
            },
            containerColor = Color(0xFFF5F5F5)
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // 1. التبويبات (Tabs)
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.White,
                    contentColor = Color(0xFF0D1B6D),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF0D1B6D),
                            height = 3.dp
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                        )
                    }
                }

                // 2. المحتوى بناءً على التبويب
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF0D1B6D)) }
                } else {
                    val pendingStatuses = listOf("pending", "priced")
                    val filteredOrders = if (selectedTabIndex == 0) {
                        orders.filter { it.order.order_status in pendingStatuses }
                    } else {
                        orders.filter { it.order.order_status !in pendingStatuses }
                    }

                    if (filteredOrders.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("لا توجد طلبات في هذا القسم", color = Color.Gray, fontSize = 16.sp)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredOrders) { orderWithItems ->
                                OrderCard(orderWithItems, userId, viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(data: OrderWithItems, userId: String, viewModel: OrdersViewModel) {
    val order = data.order
    val items = data.items
    
    // حساب الإجمالي (مجموع القطع + رسوم التوصيل)
    val totalItemsPrice = items.sumOf { it.selling_price * it.quantity }
    val grandTotal = totalItemsPrice + order.delivery_fees

    val statusColor = when(order.order_status) {
        "pending" -> Color(0xFFFFA000) // برتقالي (قيد المراجعة)
        "priced" -> Color(0xFF2196F3) // أزرق (تم التسعير - يتطلب موافقة)
        "ongoing" -> Color(0xFF9C27B0) // بنفسجي (جاري التوصيل)
        "completed" -> Color(0xFF4CAF50) // أخضر (مكتمل)
        "canceled" -> Color(0xFFF44336) // أحمر (ملغى)
        else -> Color.Gray
    }

    val statusText = when(order.order_status) {
        "pending" -> "قيد المراجعة والتسعير"
        "priced" -> "تم التسعير (بانتظار موافقتك)"
        "ongoing" -> "تمت الموافقة (جاري التوصيل)"
        "completed" -> "مكتمل"
        "canceled" -> "ملغى"
        else -> "غير معروف"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // الهيدر: المركبة والحالة
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${order.vehicle_name} - ${order.vehicle_model}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))

            // جدول القطع
            items.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• ${item.part_name} (${item.quality_type}) x${item.quantity}", color = Color.DarkGray, fontSize = 14.sp)
                    if (order.order_status != "pending") {
                        Text("${item.selling_price} ريال", fontWeight = FontWeight.Bold, color = Color(0xFF0D1B6D), fontSize = 14.sp)
                    }
                }
            }

            // الإجماليات (تظهر فقط إذا تم التسعير)
            if (order.order_status != "pending") {
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("رسوم التوصيل:", color = Color.Gray, fontSize = 14.sp)
                    Text("${order.delivery_fees} ريال", color = Color.Gray, fontSize = 14.sp)
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الإجمالي النهائي:", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text("$grandTotal ريال", fontWeight = FontWeight.ExtraBold, color = Color(0xFF4CAF50), fontSize = 16.sp)
                }
            }

            // أزرار الموافقة والرفض (تظهر فقط في حالة priced)
            if (order.order_status == "priced") {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.updateOrderStatus(order.order_id, "ongoing", userId) },
                        modifier = Modifier.weight(1f).height(45.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("موافقة واعتماد", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { viewModel.updateOrderStatus(order.order_id, "canceled", userId) },
                        modifier = Modifier.weight(1f).height(45.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
                        border = BorderStroke(1.dp, Color(0xFFF44336)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("إلغاء الطلب", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

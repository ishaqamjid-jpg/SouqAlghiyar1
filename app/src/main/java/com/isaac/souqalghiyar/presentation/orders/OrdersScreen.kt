package com.isaac.souqalghiyar.presentation.orders

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.isaac.souqalghiyar.domain.model.OrderItem
import com.isaac.souqalghiyar.domain.model.OrderWithItems

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    userId: String,
    viewModel: OrdersViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("الطلبات المعلقة", "الطلبات السابقة")

    // الجلب الآمن بمجرد دخول الشاشة وتوفر الـ ID
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            viewModel.fetchUserOrders(userId)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("طلباتي", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0D1B6D),
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = Color(0xFFF5F5F5)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = Color(0xFF0D1B6D),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF0D1B6D)
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                        )
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF0D1B6D))
                    }
                } else {
                    // تم إزالة قيد remember هنا لضمان التحديث اللحظي المباشر
                    val filteredOrders = if (selectedTab == 0) {
                        orders.filter {
                            val status = it.order.order_status.trim().lowercase()
                            status == "pending" || status == "priced"
                        }
                    } else {
                        orders.filter {
                            val status = it.order.order_status.trim().lowercase()
                            status == "ongoing" || status == "completed"
                        }
                    }

                    if (filteredOrders.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (selectedTab == 0) "لا توجد طلبات معلقة حالياً" else "لا توجد طلبات سابقة",
                                color = Color.Gray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredOrders) { orderData ->
                                OrderCard(data = orderData, viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(data: OrderWithItems, viewModel: OrdersViewModel) {
    val order = data.order
    val items = data.items
    var expanded by remember { mutableStateOf(false) }

    val itemsTotal = items.sumOf { it.selling_price * it.quantity }
    val totalInvoice = itemsTotal + order.delivery_fees

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "طلب رقم: #${order.order_number}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D1B6D),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${order.brand_name} - ${order.vehicle_name} ${order.vehicle_model}",
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                }

                val statusConfig = remember(order.order_status) {
                    when (order.order_status.trim().lowercase()) {
                        "pending" -> Pair("قيد المراجعة", Color(0xFFFFA000))
                        "priced" -> Pair("تم التسعير!", Color(0xFF2E7D32))
                        "ongoing" -> Pair("جاري التوصيل", Color(0xFF1976D2))
                        "completed" -> Pair("مكتمل", Color.Gray)
                        else -> Pair(order.order_status, Color.DarkGray)
                    }
                }

                Surface(
                    color = statusConfig.second.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusConfig.first,
                        color = statusConfig.second,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "عدد الأصناف المضافة: ${items.size}",
                    color = Color.DarkGray,
                    fontSize = 14.sp
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("تفاصيل الأصناف والفاتورة:", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.part_name} (${item.quality_type}) x${item.quantity}",
                                modifier = Modifier.weight(2f),
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                            Text(
                                text = if (order.order_status.trim().lowercase() == "pending") "قيد التسعير" else "${item.selling_price * item.quantity} ريال",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End,
                                fontSize = 14.sp,
                                color = if (order.order_status.trim().lowercase() == "pending") Color.Gray else Color.Black,
                                fontWeight = if (order.order_status.trim().lowercase() == "pending") FontWeight.Normal else FontWeight.Bold
                            )
                        }
                    }

                    if (order.order_status.trim().lowercase() != "pending") {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("رسوم التوصيل للمشوار:", fontSize = 14.sp, color = Color.DarkGray)
                            Text("${order.delivery_fees} ريال", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الإجمالي الكلي للفاتورة:", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D1B6D))
                            Text("$totalInvoice ريال", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))
                        }
                    }

                    if (order.order_status.trim().lowercase() == "priced") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.updateStatus(order.order_id, "ongoing") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("موافقة واشحن", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            OutlinedButton(
                                onClick = { viewModel.updateStatus(order.order_id, "canceled") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                                border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إلغاء الطلب", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
package com.isaac.souqalghiyar.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    userId: String,
    viewModel: NotificationsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsState()
    
    // متغير لتخزين الإشعار المفتوح لقرائته (Dialog)
    var selectedNotification by remember { mutableStateOf<NotificationItem?>(null) }

    LaunchedEffect(userId) {
        viewModel.fetchNotifications(userId)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("الإشعارات", fontWeight = FontWeight.Bold, color = Color(0xFFE53935)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1E1E1E))
                )
            },
            containerColor = Color(0xFF121212)
        ) { innerPadding ->
            
            if (notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("لا توجد إشعارات حالياً", color = Color.Gray, fontSize = 18.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        val bgColor = if (notif.isRead) Color(0xFF1E1E1E) else Color(0xFF2C2C2C)
                        val indicatorColor = if (notif.isRead) Color.Transparent else Color(0xFFE53935)

                        Card(
                            // عند الضغط على الكارد يتم فتح رسالة القراءة (Dialog)
                            modifier = Modifier.fillMaxWidth().clickable { 
                                selectedNotification = notif 
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(10.dp).background(indicatorColor, RoundedCornerShape(50)))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(notif.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp, maxLines = 1)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(notif.message, color = Color.LightGray, fontSize = 14.sp, maxLines = 2)
                                }
                                // زر المسح اليدوي المستقل للإشعار
                                IconButton(onClick = { viewModel.deleteNotification(notif.id, notif.type) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE53935))
                                }
                            }
                        }
                    }
                }
            }

            // --- نافذة قراءة الإشعار (Dialog) ---
            selectedNotification?.let { notif ->
                AlertDialog(
                    onDismissRequest = {
                        // عند الخروج خارج الرسالة (أو الإغلاق)
                        if (notif.type == "order") {
                            viewModel.deleteNotification(notif.id, notif.type) // مسح طلب
                        } else {
                            if (!notif.isRead) viewModel.markAsRead(notif.id, notif.type) // مجرد قراءة إعلان
                        }
                        selectedNotification = null
                    },
                    containerColor = Color(0xFF2C2C2C),
                    title = { 
                        Text(notif.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp) 
                    },
                    text = { 
                        Text(notif.message, color = Color.White, fontSize = 16.sp) 
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (notif.type == "order") {
                                    viewModel.deleteNotification(notif.id, notif.type) // يتم المسح
                                } else {
                                    if (!notif.isRead) viewModel.markAsRead(notif.id, notif.type) // يتم التأشير كمقروء للإعلانات
                                }
                                selectedNotification = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                        ) {
                            Text("تم القراءة وإغلاق", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }
}

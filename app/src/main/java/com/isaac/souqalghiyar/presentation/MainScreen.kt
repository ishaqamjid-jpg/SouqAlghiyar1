package com.isaac.souqalghiyar.presentation.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userId: String,
    navigateToRequestParts: () -> Unit,
    navigateToOrders: () -> Unit
) {
    // متغيرات الحالة (States)
    var carName by remember { mutableStateOf("") }
    var carModel by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // إجبار اتجاه الواجهة RTL للغة العربية
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("أهلاً بكم في سوق الغيار", fontWeight = FontWeight.Bold) },
                    actions = {
                        // زر للذهاب إلى شاشة الطلبات المعلقة والمنتهية
                        IconButton(onClick = navigateToOrders) {
                            Icon(Icons.Default.ListAlt, contentDescription = "الطلبات", tint = Color.White)
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                // 1. كارد الإعلانات المتحرك
                AnimatedAdsCard()

                Spacer(modifier = Modifier.height(24.dp))

                // 2. خانات اسم السيارة والموديل (مغلقة ولا يمكن تعبئتها يدوياً)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = carName,
                        onValueChange = {},
                        label = { Text("اسم السيارة") },
                        enabled = false, // يمنع التعديل اليدوي
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Black,
                            disabledBorderColor = Color.Gray,
                            disabledLabelColor = Color.DarkGray
                        )
                    )
                    OutlinedTextField(
                        value = carModel,
                        onValueChange = {},
                        label = { Text("الموديل") },
                        enabled = false, // يمنع التعديل اليدوي
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Black,
                            disabledBorderColor = Color.Gray,
                            disabledLabelColor = Color.DarkGray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. خانة إدراج صورة رقم القعّادة
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE0E0E0))
                        .clickable { /* لاحقاً كود فتح الكاميرا أو المعرض */ },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "الكاميرا",
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("إدراج صورة لرقم القعّادة", color = Color.DarkGray)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4. زر عرض نوع المركبة (تحليل AI / Internet)
                Button(
                    onClick = {
                        isAnalyzing = true
                        // محاكاة الاتصال بالنت أو الذكاء الاصطناعي لتحليل الصورة
                        coroutineScope.launch {
                            delay(2500) // انتظار وهمي ثانيتين ونصف
                            carName = "تويوتا لاندكروزر" // تعبئة أوتوماتيكية
                            carModel = "2023" // تعبئة أوتوماتيكية
                            isAnalyzing = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5))
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("عرض نوع المركبة", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 5. زر طلب قطع الغيار في أسفل الشاشة
                Button(
                    onClick = navigateToRequestParts,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D1B6D)),
                    // يمكن جعل الزر مفعل فقط بعد أن يتعرف على نوع السيارة
                    enabled = carName.isNotEmpty() 
                ) {
                    Text("طلب قطع غيار", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// مكون منفصل لبطاقة الإعلانات المتحركة (نبض خفيف)
@Composable
fun AnimatedAdsCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "ads_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFA000))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "مساحة إعلانية\n(عروض قطع الغيار والزيوت)",
                textAlign = TextAlign.Center,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}

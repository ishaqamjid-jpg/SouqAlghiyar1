package com.isaac.souqalghiyar.presentation.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userId: String,
    viewModel: MainViewModel = hiltViewModel(), // ربط الـ ViewModel الخاص بالإعلانات
    navigateToRequestParts: (String, String, String) -> Unit, // نمرر اسم وموديل السيارة ورابط الصورة
    navigateToOrders: () -> Unit
) {
    // جمع قائمة الإعلانات من قاعدة البيانات (إذا كانت جاهزة في الـ ViewModel)
    val adsList by viewModel.adsList.collectAsState()

    // متغيرات حالة حقول إدخال المركبة
    var carName by remember { mutableStateOf("") }
    var carModel by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var vinPicUrl by remember { mutableStateOf("") } // سيحفظ رابط الصورة مستقبلاً
    
    val coroutineScope = rememberCoroutineScope()

    // إجبار اتجاه الواجهة RTL لتتناسب مع اللغة العربية
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("أهلاً بكم في سوق الغيار", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = navigateToOrders) {
                            Icon(Icons.Default.ListAlt, contentDescription = "الطلبات", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0D1B6D),
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
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
                
                // 1. بطاقة الإعلانات المتحركة
                AnimatedAdsCard(ads = adsList)

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
                        .clickable { 
                            // لاحقاً هنا كود لفتح الكاميرا ورفع الصورة للفايربيز
                            vinPicUrl = "mock_image_url" 
                        },
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

                // 4. زر عرض نوع المركبة (تحليل مؤقت كأنه ذكاء اصطناعي)
                Button(
                    onClick = {
                        isAnalyzing = true
                        // محاكاة الاتصال بالنت أو الذكاء الاصطناعي لتحليل الصورة
                        coroutineScope.launch {
                            delay(2500) // انتظار وهمي لمدة ثانيتين ونصف
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

                // 5. زر طلب قطع الغيار في أسفل الشاشة (يُفعل فقط إذا تعرّف على السيارة)
                Button(
                    onClick = { 
                        // تمرير البيانات المجلوبة للشاشة التالية الخاصة بجدول القطع
                        navigateToRequestParts(carName, carModel, vinPicUrl.ifEmpty { "no_image" }) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D1B6D)),
                    enabled = carName.isNotEmpty() 
                ) {
                    Text("طلب قطع غيار", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// مكون منفصل لبطاقة الإعلانات المتحركة
@Composable
fun AnimatedAdsCard(ads: List<Advertisement>) {
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

    // مؤشر للإعلان الحالي
    var currentIndex by remember { mutableIntStateOf(0) }

    // تغيير الإعلان كل 3 ثوانٍ إذا كان هناك أكثر من إعلان متاح
    LaunchedEffect(ads) {
        if (ads.isNotEmpty() && ads.size > 1) {
            while (true) {
                delay(3000)
                currentIndex = (currentIndex + 1) % ads.size
            }
        }
    }

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
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (ads.isEmpty()) {
                // حالة افتراضية حتى يتم جلب البيانات
                Text(
                    text = "جارِ تحميل العروض...",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            } else {
                // أنيميشن ناعم عند التبديل بين نصوص الإعلانات
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
                    label = "ad_transition"
                ) { targetIndex ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = ads[targetIndex].title,
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = ads[targetIndex].content,
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

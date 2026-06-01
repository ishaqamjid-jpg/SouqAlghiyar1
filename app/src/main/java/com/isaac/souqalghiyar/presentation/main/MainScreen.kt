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
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.isaac.souqalghiyar.domain.model.Advertisement
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userId: String,
    viewModel: MainViewModel = hiltViewModel(),
    navigateToRequestParts: (String, String, String) -> Unit,
    navigateToOrders: () -> Unit
) {
    val adsList by viewModel.adsList.collectAsState()
    val context = LocalContext.current

    var carName by remember { mutableStateOf("") }
    var carModel by remember { mutableStateOf("") }
    var extractedVinNumber by remember { mutableStateOf("") } // النص الذي سيستخرجه الذكاء الاصطناعي من الصورة

    var isImageUploaded by remember { mutableStateOf(false) } // للتحكم في شكل مربع الكاميرا
    var isAnalyzing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

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

                // 2. خانات اسم السيارة والموديل (مغلقة وتتعبأ آلياً)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = carName,
                        onValueChange = {},
                        label = { Text("اسم السيارة") },
                        enabled = false,
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
                        enabled = false,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Black,
                            disabledBorderColor = Color.Gray,
                            disabledLabelColor = Color.DarkGray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. مربع التقاط الصورة (كما طلبت، يبقى لرفع الصورة فقط)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isImageUploaded) Color(0xFFC8E6C9) else Color(0xFFE0E0E0)) // يتغير للأخضر الفاتح عند الإرفاق
                        .clickable {
                            // محاكاة قيام المستخدم بفتح الكاميرا والتقاط صورة
                            isImageUploaded = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isImageUploaded) Icons.Default.CheckCircle else Icons.Default.PhotoCamera,
                            contentDescription = "الكاميرا",
                            tint = if (isImageUploaded) Color(0xFF2E7D32) else Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isImageUploaded) "تم إرفاق الصورة بنجاح" else "التقط صورة لرقم القعّادة",
                            color = if (isImageUploaded) Color(0xFF2E7D32) else Color.DarkGray,
                            fontWeight = if (isImageUploaded) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4. زر الذكاء الاصطناعي (يستخرج البيانات والنص ويرمي الصورة)
                Button(
                    onClick = {
                        if (!isImageUploaded) {
                            Toast.makeText(context, "يرجى التقاط صورة رقم القعادة أولاً", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isAnalyzing = true
                        coroutineScope.launch {
                            delay(2500) // محاكاة وقت تحليل ML Kit للصورة
                            carName = "تويوتا لاندكروزر"
                            carModel = "2023"
                            extractedVinNumber = "JTD1234567890ABCD" // 👈 هنا استخرجنا الرقم كنص ولن نحتاج للصورة بعد الآن
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
                        Text("تحليل الصورة واستخراج البيانات", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 5. زر طلب قطع الغيار في أسفل الشاشة
                Button(
                    onClick = {
                        // نمرر الـ Text الذي استخرجناه من الصورة (extractedVinNumber) بدلاً من الصورة نفسها
                        navigateToRequestParts(carName, carModel, extractedVinNumber.ifEmpty { "غير محدد" })
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

    var currentIndex by remember { mutableIntStateOf(0) }

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
                Text(
                    text = "جارِ تحميل العروض...",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            } else {
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
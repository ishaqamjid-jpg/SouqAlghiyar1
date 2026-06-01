package com.isaac.souqalghiyar.presentation.main

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.isaac.souqalghiyar.domain.model.Advertisement
import kotlinx.coroutines.delay

val PrimaryBlue = Color(0xFF0D1B6D)
val AccentBlue = Color(0xFF42A5F5)
val BackgroundGray = Color(0xFFF5F5F5)
val SuccessGreen = Color(0xFF2E7D32)
val LightGreen = Color(0xFFC8E6C9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userId: String,
    viewModel: MainViewModel = hiltViewModel(),
    navigateToRequestParts: (String, String, String, String, String) -> Unit,
    navigateToOrders: () -> Unit
) {
    val adsList by viewModel.adsList.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val context = LocalContext.current

    var carMake by remember { mutableStateOf("") }
    var carModel by remember { mutableStateOf("") }
    var carYear by remember { mutableStateOf("") }
    var carMadeIn by remember { mutableStateOf("") }
    var vinNumber by remember { mutableStateOf("") }

    // متغير لحفظ الـ URI للصورة المختارة من الجهاز
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    // متغير لحفظ الصورة كـ Bitmap لتمريرها للذكاء الاصطناعي
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Launcher لاختيار صورة من الاستوديو
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        // تحويل الـ URI إلى Bitmap بمجرد اختيار الصورة
        if (uri != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    selectedBitmap = ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    selectedBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "فشل في قراءة الصورة", Toast.LENGTH_SHORT).show()
                selectedBitmap = null
            }
        }
    }

    val isRequiredFieldsFilled = carMake.isNotBlank() &&
            carModel.isNotBlank() &&
            carYear.isNotBlank() &&
            carMadeIn.isNotBlank()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("سوق الغيار اليمن", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) },
                    actions = {
                        IconButton(onClick = navigateToOrders) {
                            Icon(Icons.Default.ListAlt, contentDescription = "طلباتي", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PrimaryBlue,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    modifier = Modifier.shadow(8.dp)
                )
            },
            containerColor = BackgroundGray
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                AnimatedAdsCard(ads = adsList)
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "بيانات المركبة",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                CarDetailsFields(
                    make = carMake, onMakeChange = { carMake = it },
                    model = carModel, onModelChange = { carModel = it },
                    year = carYear, onYearChange = { carYear = it },
                    madeIn = carMadeIn, onMadeInChange = { carMadeIn = it },
                    vin = vinNumber, onVinChange = { vinNumber = it }
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "لتعبئة الخانات تلقائياً يرجى إرفاق صورة لملصق الشاصي",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // مربع التقاط/اختيار الصورة
                PhotoPickerBox(
                    isUploaded = selectedBitmap != null,
                    onClick = {
                        // فتح الاستوديو لاختيار صورة (image/*)
                        imagePickerLauncher.launch("image/*")
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // زر التحليل الآلي
                AnalyzeButton(
                    isImageUploaded = selectedBitmap != null,
                    isAnalyzing = isAnalyzing,
                    onClick = {
                        selectedBitmap?.let { bitmap ->
                            viewModel.analyzeVinImageFromBitmap(
                                bitmap = bitmap,
                                onSuccess = { make, model, year, madeIn, vin ->
                                    carMake = make
                                    carModel = model
                                    carYear = year
                                    carMadeIn = madeIn
                                    vinNumber = vin
                                    Toast.makeText(context, "تم استخراج البيانات بنجاح", Toast.LENGTH_LONG).show()
                                },
                                onError = { errorMsg ->
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(40.dp))
                Spacer(modifier = Modifier.weight(1f))

                AnimatedVisibility(visible = isRequiredFieldsFilled) {
                    Button(
                        onClick = {
                            navigateToRequestParts(carMake, carModel, carYear, carMadeIn, vinNumber.ifEmpty { "غير محدد" })
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(bottom = 8.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("طلب قطع غيار", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ... (يرجى إبقاء باقي الدوال المساعدة كما هي: CarDetailsFields, PhotoPickerBox, AnalyzeButton, AnimatedAdsCard)
@Composable
fun CarDetailsFields(
    make: String, onMakeChange: (String) -> Unit,
    model: String, onModelChange: (String) -> Unit,
    year: String, onYearChange: (String) -> Unit,
    madeIn: String, onMadeInChange: (String) -> Unit,
    vin: String, onVinChange: (String) -> Unit
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val defaultTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedBorderColor = PrimaryBlue,
        unfocusedBorderColor = Color.Gray,
        focusedLabelColor = PrimaryBlue,
        unfocusedLabelColor = Color.DarkGray,
        cursorColor = PrimaryBlue
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = make, onValueChange = onMakeChange,
                label = { Text("الماركة") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = defaultTextFieldColors,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = model, onValueChange = onModelChange,
                label = { Text("الموديل") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = defaultTextFieldColors,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = year, onValueChange = onYearChange,
                label = { Text("السنة") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = defaultTextFieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = madeIn, onValueChange = onMadeInChange,
                label = { Text("مكان التصنيع") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = defaultTextFieldColors,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )
        }

        OutlinedTextField(
            value = vin, onValueChange = onVinChange,
            label = { Text("رقم القعادة / الشاصي (17 خانة - اختياري)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = defaultTextFieldColors,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done, keyboardType = KeyboardType.Ascii),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun PhotoPickerBox(isUploaded: Boolean, onClick: () -> Unit) {
    val borderColor = if (isUploaded) SuccessGreen else Color.Gray.copy(alpha = 0.5f)
    val bgColor = if (isUploaded) LightGreen else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = if (isUploaded) Icons.Default.CheckCircle else Icons.Default.PhotoCamera,
                contentDescription = "الكاميرا",
                tint = if (isUploaded) SuccessGreen else AccentBlue,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isUploaded) "تم اختيار الصورة بنجاح" else "انقر لاختيار صورة ملصق الشاصي",
                color = if (isUploaded) SuccessGreen else Color.DarkGray,
                fontWeight = if (isUploaded) FontWeight.Bold else FontWeight.Normal,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AnalyzeButton(isImageUploaded: Boolean, isAnalyzing: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current

    Button(
        onClick = {
            if (!isImageUploaded) {
                Toast.makeText(context, "يرجى اختيار صورة ملصق الشاصي أولاً", Toast.LENGTH_SHORT).show()
            } else {
                onClick()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(colors = listOf(AccentBlue, Color(0xFF2196F3))),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("استخراج البيانات من الصورة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun AnimatedAdsCard(ads: List<Advertisement>) {
    val infiniteTransition = rememberInfiniteTransition(label = "ads_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.97f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "scale"
    )
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(ads) {
        if (ads.isNotEmpty() && ads.size > 1) {
            while (true) {
                delay(4000)
                currentIndex = (currentIndex + 1) % ads.size
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(140.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2994A))
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors = listOf(Color(0xFFF2994A), Color(0xFFF2C94C)))).padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (ads.isEmpty()) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
            } else {
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(600)) },
                    label = "ad_transition"
                ) { targetIndex ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = ads[targetIndex].title, textAlign = TextAlign.Center, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 26.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = ads[targetIndex].content, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
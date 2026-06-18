package com.isaac.souqalghiyar.presentation.main

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
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
    navigateToOrders: (String) -> Unit
) {
    val adsList by viewModel.adsList.collectAsState()
    val brandsList by viewModel.brandsList.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val isLoadingData by viewModel.isLoadingData.collectAsState()
    
    // حالة الإشعار
    val hasPendingOrders by viewModel.hasPendingOrders.collectAsState()
    
    val context = LocalContext.current

    // جلب حالة الإشعارات أول ما تفتح الشاشة
    LaunchedEffect(userId) {
        viewModel.checkPendingOrders(userId)
    }

    var brandName by remember { mutableStateOf("") }
    var vehicleModel by remember { mutableStateOf("") } 
    var vehicleYear by remember { mutableStateOf("") }  
    var manufacture by remember { mutableStateOf("") }
    var vinNumber by remember { mutableStateOf("") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
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

    val isRequiredFieldsFilled = brandName.isNotBlank() && vehicleModel.isNotBlank() && vehicleYear.isNotBlank() && manufacture.isNotBlank()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("سوق الغيار اليمن", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) },
                    actions = {
                        IconButton(onClick = { navigateToOrders(userId) }) {
                            // إضافة الـ BadgedBox لظهور علامة التنبيه (النقطة الحمراء)
                            BadgedBox(
                                badge = {
                                    if (hasPendingOrders) {
                                        Badge(
                                            containerColor = Color.Red,
                                            contentColor = Color.White,
                                            modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                                        ) {
                                            Text("!", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ListAlt, contentDescription = "طلباتي", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
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
            if (isLoadingData) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("جاري تحديث البيانات...", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // استدعاء مكون الإعلانات الجديد (بالسحب والتحريك التلقائي)
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
                        brand = brandName, onBrandChange = { brandName = it }, brandsList = brandsList,
                        model = vehicleModel, onModelChange = { vehicleModel = it },
                        year = vehicleYear, onYearChange = { vehicleYear = it },
                        madeIn = manufacture, onMadeInChange = { manufacture = it },
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

                    PhotoPickerBox(
                        isUploaded = selectedBitmap != null,
                        onClick = { imagePickerLauncher.launch("image/*") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AnalyzeButton(
                        isImageUploaded = selectedBitmap != null,
                        isAnalyzing = isAnalyzing,
                        onClick = {
                            selectedBitmap?.let { bitmap ->
                                viewModel.analyzeVinImageFromBitmap(
                                    bitmap = bitmap,
                                    onSuccess = { brand, model, year, madeIn, vin ->
                                        brandName = brand
                                        vehicleModel = model
                                        vehicleYear = year
                                        manufacture = madeIn
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
                    
                    Button(
                        onClick = {
                            navigateToRequestParts(brandName, vehicleModel, vehicleYear, manufacture, vinNumber.ifEmpty { "غير محدد" })
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(bottom = 8.dp)
                            .shadow(if(isRequiredFieldsFilled) 8.dp else 0.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        enabled = isRequiredFieldsFilled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            disabledContainerColor = Color.LightGray
                        )
                    ) {
                        Text(
                            "طلب قطع غيار", 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isRequiredFieldsFilled) Color.White else Color.DarkGray
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

// دالة الحقول (لم تتغير، تبقى كما هي)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarDetailsFields(
    brand: String, onBrandChange: (String) -> Unit, brandsList: List<String>,
    model: String, onModelChange: (String) -> Unit,
    year: String, onYearChange: (String) -> Unit,
    madeIn: String, onMadeInChange: (String) -> Unit,
    vin: String, onVinChange: (String) -> Unit
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var expandedBrand by remember { mutableStateOf(false) }
    var expandedYear by remember { mutableStateOf(false) }
    var expandedMadeIn by remember { mutableStateOf(false) }

    val yearsList = (2000..2026).map { it.toString() }.reversed()
    val madeInOptions = listOf("أمريكي", "خليجي", "وارد آخر")

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
            ExposedDropdownMenuBox(
                expanded = expandedBrand,
                onExpandedChange = { expandedBrand = !expandedBrand },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = brand,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("الماركة *") },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = defaultTextFieldColors,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBrand) },
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandedBrand,
                    onDismissRequest = { expandedBrand = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    brandsList.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt, color = Color.Black) },
                            onClick = {
                                onBrandChange(opt)
                                expandedBrand = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = model, onValueChange = onModelChange,
                label = { Text("نوع الموديل *") },
                placeholder = { Text("مثل: كورولا") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = defaultTextFieldColors,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExposedDropdownMenuBox(
                expanded = expandedYear,
                onExpandedChange = { expandedYear = !expandedYear },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = year,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("السنة *") },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = defaultTextFieldColors,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedYear) },
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandedYear,
                    onDismissRequest = { expandedYear = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    yearsList.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt, color = Color.Black) },
                            onClick = {
                                onYearChange(opt)
                                expandedYear = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = expandedMadeIn,
                onExpandedChange = { expandedMadeIn = !expandedMadeIn },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = madeIn,
                    onValueChange = onMadeInChange,
                    label = { Text("مكان التصنيع *") },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = defaultTextFieldColors,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMadeIn) },
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandedMadeIn,
                    onDismissRequest = { expandedMadeIn = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    madeInOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt, color = Color.Black) },
                            onClick = {
                                onMadeInChange(opt)
                                expandedMadeIn = false
                            }
                        )
                    }
                }
            }
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

// دالة PhotoPickerBox (لم تتغير)
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

// دالة AnalyzeButton (لم تتغير)
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
        modifier = Modifier.fillMaxWidth().height(55.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(colors = listOf(AccentBlue, Color(0xFF2196F3))), shape = RoundedCornerShape(14.dp)),
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

// الكارد الجديد للإعلانات (يدعم السحب اليدوي + الانتقال التلقائي كل 3 ثواني)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedAdsCard(ads: List<Advertisement>) {
    // إذا كانت القائمة فارغة، إظهار شاشة تحميل مصغرة
    if (ads.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth().height(140.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2994A))
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors = listOf(Color(0xFFF2994A), Color(0xFFF2C94C)))),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
            }
        }
        return
    }

    // مكون HorizontalPager يدعم السحب لليمين واليسار (Swipe)
    val pagerState = rememberPagerState(pageCount = { ads.size })

    // مؤقت للانتقال التلقائي كل 3 ثوانٍ
    LaunchedEffect(pagerState) {
        while (true) {
            delay(3000) // 3 ثواني بدلاً من 4
            if (ads.size > 1) {
                val nextPage = (pagerState.currentPage + 1) % ads.size
                // انتقال ناعم
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2994A))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                
                AsyncImage(
                    model = ads[page].image_url,
                    contentDescription = "الإعلان",
                    contentScale = ContentScale.Crop, 
                    modifier = Modifier.fillMaxSize()
                )
                
                // طبقة التعتيم
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                
                // النصوص
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = ads[page].title, 
                        textAlign = TextAlign.Center, 
                        color = Color.White, 
                        fontWeight = FontWeight.ExtraBold, 
                        fontSize = 20.sp, 
                        lineHeight = 26.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = ads[page].target_url ?: "", 
                        textAlign = TextAlign.Center, 
                        color = Color.White.copy(alpha = 0.9f), 
                        fontWeight = FontWeight.Medium, 
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

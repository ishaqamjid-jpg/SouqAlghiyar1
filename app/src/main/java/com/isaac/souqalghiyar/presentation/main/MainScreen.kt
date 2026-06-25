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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.isaac.souqalghiyar.R
import com.isaac.souqalghiyar.domain.model.Advertisement
import kotlinx.coroutines.delay

val PrimaryRed = Color(0xFFE53935)
val DarkBackground = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val TextWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFFAAAAAA)
val SuccessGreen = Color(0xFF388E3C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userId: String,
    viewModel: MainViewModel = hiltViewModel(),
    navigateToRequestParts: (String, String, String, String, String) -> Unit,
    navigateToOrders: (String) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val adsList by viewModel.adsList.collectAsState()
    val brandsList by viewModel.brandsList.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val isSearchingVin by viewModel.isSearchingVin.collectAsState()
    val isLoadingData by viewModel.isLoadingData.collectAsState()
    val hasPendingOrders by viewModel.hasPendingOrders.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(userId) {
        viewModel.checkPendingOrders(userId)
        viewModel.fetchUserData(userId) // جلب بيانات المستخدم فور الدخول
    }

    var brandName by remember { mutableStateOf("") }
    var vehicleModel by remember { mutableStateOf("") }
    var vehicleYear by remember { mutableStateOf("") }
    var manufacture by remember { mutableStateOf("") }
    var vinNumber by remember { mutableStateOf("") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }

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

    // 1. شرط إيقاف الحساب (النافذة الإجبارية)
    if (currentUser != null && currentUser!!.number_of_rejections > 2.0) {
        AlertDialog(
            onDismissRequest = { /* لا يفعل شيء، النافذة لا تُغلق */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = {
                Text(
                    text = "تنبيه إيقاف الحساب",
                    color = PrimaryRed,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = "تم ايقاف حسابك بسبب تكرار رفض الفواتير اكثر من مرتين . لتفعيل حسابك يجب فرض رسوم مبلغ وقدره ٢٠٠٠ ريال يمني .\n\nطريقه تسديد الرسوم :\nحواله الى محفظه بجيب الى حساب مشترك رقم 558933 \nوارسال الاشعار وتساب الى الرقم 777979719",
                    color = TextWhite,
                    fontSize = 16.sp,
                    lineHeight = 26.sp
                )
            },
            confirmButton = {}, // إزالة الزر لكي لا يتمكن من الخروج
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text("سوق الغيار", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    },
                    navigationIcon = {
                        IconButton(onClick = { showAboutDialog = true }) {
                            Icon(Icons.Default.Info, contentDescription = "حول النظام", tint = TextWhite, modifier = Modifier.size(26.dp))
                        }
                    },
                    actions = {
                        IconButton(onClick = { navigateToOrders(userId) }) {
                            BadgedBox(
                                badge = {
                                    if (hasPendingOrders) {
                                        Badge(
                                            containerColor = PrimaryRed,
                                            contentColor = TextWhite,
                                            modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                                        ) {
                                            Text("!", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ListAlt, contentDescription = "طلباتي", tint = TextWhite, modifier = Modifier.size(26.dp))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = PrimaryRed,
                        actionIconContentColor = TextWhite
                    ),
                    modifier = Modifier.shadow(8.dp)
                )
            },
            containerColor = DarkBackground
        ) { innerPadding ->
            if (isLoadingData) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("جاري تحديث البيانات...", color = PrimaryRed, fontWeight = FontWeight.Bold)
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

                    // 2. رسالة الترحيب باسم المستخدم
                    if (currentUser != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "أهلاً بكم، ",
                                color = TextGray,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = currentUser!!.display_name,
                                color = PrimaryRed,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    AnimatedAdsCard(ads = adsList)
                    Spacer(modifier = Modifier.height(24.dp))

                    Text("بيانات المركبة", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, color = PrimaryRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    CarDetailsFields(
                        brand = brandName, onBrandChange = { brandName = it }, brandsList = brandsList,
                        model = vehicleModel, onModelChange = { vehicleModel = it },
                        year = vehicleYear, onYearChange = { vehicleYear = it },
                        madeIn = manufacture, onMadeInChange = { manufacture = it },
                        vin = vinNumber, onVinChange = { vinNumber = it },
                        isSearchingVin = isSearchingVin,
                        onSearchVin = { searchVin ->
                            viewModel.searchByVin(
                                vin = searchVin,
                                onSuccess = { fetchedBrand, fetchedModel, fetchedYear, fetchedMadeIn ->
                                    brandName = fetchedBrand
                                    vehicleModel = fetchedModel
                                    vehicleYear = fetchedYear
                                    manufacture = fetchedMadeIn
                                    Toast.makeText(context, "تم العثور على بيانات المركبة", Toast.LENGTH_SHORT).show()
                                },
                                onError = { errorMsg ->
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = SurfaceDark)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("لتعبئة الخانات تلقائياً يرجى إرفاق صورة لملصق الشاصي", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = TextGray, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    PhotoPickerBox(
                        isUploaded = selectedBitmap != null,
                        onClick = { imagePickerLauncher.launch("image/*") },
                        onClear = {
                            selectedBitmap = null
                            selectedImageUri = null
                        }
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
                            navigateToRequestParts(brandName, vehicleModel, vehicleYear, manufacture, vinNumber.ifEmpty { "غير مححدد" })
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(bottom = 8.dp)
                            .shadow(if (isRequiredFieldsFilled) 8.dp else 0.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        enabled = isRequiredFieldsFilled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryRed,
                            disabledContainerColor = SurfaceDark
                        )
                    ) {
                        Text("طلب قطع غيار", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = if (isRequiredFieldsFilled) TextWhite else TextGray)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            if (showAboutDialog) {
                AboutSystemDialog(onDismiss = { showAboutDialog = false })
            }
        }
    }
}

// ... (تكملة الدوال الباقية بالأسفل مثل CarDetailsFields و PhotoPickerBox و AnalyzeButton و AnimatedAdsCard و AboutSystemDialog لم تتغير)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarDetailsFields(
    brand: String, onBrandChange: (String) -> Unit, brandsList: List<String>,
    model: String, onModelChange: (String) -> Unit,
    year: String, onYearChange: (String) -> Unit,
    madeIn: String, onMadeInChange: (String) -> Unit,
    vin: String, onVinChange: (String) -> Unit,
    isSearchingVin: Boolean,
    onSearchVin: (String) -> Unit
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var expandedBrand by remember { mutableStateOf(false) }
    var expandedYear by remember { mutableStateOf(false) }
    var expandedMadeIn by remember { mutableStateOf(false) }

    val yearsList = (2000..2026).map { it.toString() }.reversed()
    val madeInOptions = listOf("الولايات المتحدة الأمريكية", "مواصفات خليجي", "اليابان", "المانيا", "كندا", "غير معروف")

    val defaultTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
        focusedBorderColor = TextWhite, unfocusedBorderColor = TextWhite,
        focusedLabelColor = PrimaryRed, unfocusedLabelColor = TextWhite,
        cursorColor = PrimaryRed,
        focusedContainerColor = SurfaceDark.copy(alpha = 0.5f),
        unfocusedContainerColor = SurfaceDark.copy(alpha = 0.3f)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExposedDropdownMenuBox(expanded = expandedBrand, onExpandedChange = { expandedBrand = !expandedBrand }, modifier = Modifier.weight(1f)) {
                OutlinedTextField(value = brand, onValueChange = {}, readOnly = true, label = { Text("الماركة *") }, modifier = Modifier.menuAnchor().fillMaxWidth(), colors = defaultTextFieldColors, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBrand) }, shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenu(expanded = expandedBrand, onDismissRequest = { expandedBrand = false }, modifier = Modifier.background(SurfaceDark)) {
                    brandsList.forEach { opt -> DropdownMenuItem(text = { Text(opt, color = TextWhite) }, onClick = { onBrandChange(opt); expandedBrand = false }) }
                }
            }

            OutlinedTextField(value = model, onValueChange = onModelChange, label = { Text("نوع الموديل *") }, placeholder = { Text("مثل: كورولا", color = TextGray.copy(alpha = 0.5f)) }, modifier = Modifier.weight(1f), singleLine = true, colors = defaultTextFieldColors, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next), shape = RoundedCornerShape(12.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExposedDropdownMenuBox(expanded = expandedYear, onExpandedChange = { expandedYear = !expandedYear }, modifier = Modifier.weight(1f)) {
                OutlinedTextField(value = year, onValueChange = {}, readOnly = true, label = { Text("السنة *") }, modifier = Modifier.menuAnchor().fillMaxWidth(), colors = defaultTextFieldColors, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedYear) }, shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenu(expanded = expandedYear, onDismissRequest = { expandedYear = false }, modifier = Modifier.background(SurfaceDark)) {
                    yearsList.forEach { opt -> DropdownMenuItem(text = { Text(opt, color = TextWhite) }, onClick = { onYearChange(opt); expandedYear = false }) }
                }
            }

            ExposedDropdownMenuBox(expanded = expandedMadeIn, onExpandedChange = { expandedMadeIn = !expandedMadeIn }, modifier = Modifier.weight(1f)) {
                OutlinedTextField(value = madeIn, onValueChange = onMadeInChange, label = { Text("مكان التصنيع *") }, modifier = Modifier.menuAnchor().fillMaxWidth(), colors = defaultTextFieldColors, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMadeIn) }, shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenu(expanded = expandedMadeIn, onDismissRequest = { expandedMadeIn = false }, modifier = Modifier.background(SurfaceDark)) {
                    madeInOptions.forEach { opt -> DropdownMenuItem(text = { Text(opt, color = TextWhite) }, onClick = { onMadeInChange(opt); expandedMadeIn = false }) }
                }
            }
        }

        OutlinedTextField(
            value = vin,
            onValueChange = onVinChange,
            label = { Text("رقم القعادة / الشاصي (17 خانة)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = defaultTextFieldColors,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done, keyboardType = KeyboardType.Ascii),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                if (isSearchingVin) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PrimaryRed, strokeWidth = 2.dp)
                } else if (vin.isNotEmpty()) {
                    IconButton(onClick = { onSearchVin(vin) }) {
                        Icon(Icons.Default.Search, contentDescription = "بحث", tint = PrimaryRed)
                    }
                }
            }
        )
    }
}

@Composable
fun PhotoPickerBox(isUploaded: Boolean, onClick: () -> Unit, onClear: () -> Unit) {
    val borderColor = if (isUploaded) SuccessGreen else TextWhite
    val bgColor = if (isUploaded) SuccessGreen.copy(alpha = 0.1f) else SurfaceDark.copy(alpha = 0.5f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick, enabled = !isUploaded),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = if (isUploaded) Icons.Default.CheckCircle else Icons.Default.PhotoCamera,
                contentDescription = "الكاميرا",
                tint = if (isUploaded) SuccessGreen else TextWhite,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isUploaded) "تم اختيار الصورة بنجاح" else "انقر لاختيار صورة ملصق الشاصي",
                color = if (isUploaded) SuccessGreen else TextWhite,
                fontWeight = if (isUploaded) FontWeight.Bold else FontWeight.Normal,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }

        if (isUploaded) {
            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(SurfaceDark.copy(alpha = 0.8f), CircleShape)
                    .size(32.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "حذف الصورة", tint = PrimaryRed, modifier = Modifier.size(20.dp))
            }
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
        modifier = Modifier.fillMaxWidth().height(55.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.horizontalGradient(colors = listOf(PrimaryRed, Color(0xFF8E0000))),
                shape = RoundedCornerShape(14.dp)
            ),
            contentAlignment = Alignment.Center
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(color = TextWhite, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TextWhite, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("استخراج البيانات من الصورة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedAdsCard(ads: List<Advertisement>) {
    if (ads.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth().height(140.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryRed, modifier = Modifier.size(28.dp))
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { ads.size })

    LaunchedEffect(pagerState) {
        while (true) {
            delay(3000)
            if (ads.size > 1) {
                val nextPage = (pagerState.currentPage + 1) % ads.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(140.dp).border(1.dp, SurfaceDark, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(model = ads[page].image_url, contentDescription = "الإعلان", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))
                ))
                Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Text(text = ads[page].title, textAlign = TextAlign.Center, color = TextWhite, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 26.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = ads[page].target_url ?: "", textAlign = TextAlign.Center, color = PrimaryRed, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun AboutSystemDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = PrimaryRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .size(90.dp)
                        .shadow(10.dp, CircleShape, spotColor = PrimaryRed),
                    shape = CircleShape,
                    color = DarkBackground
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo3),
                        contentDescription = "الشعار",
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "خدمة العملاء",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryRed,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("+967-777979719", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("+967-736373788", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ishaq.amjid@gmail.com", color = TextWhite, fontSize = 14.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اليمن - صنعاء", color = TextWhite, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(15.dp))

                Text(
                    text = "حول النظام",
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryRed,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "سوق الغيار خدمه تابعه لشركه الحبيب للتجاره العامه  وهو تطبيق لشراء قطع غيار لكل انواع المركبات وايضا عبر علاقات مع جميع محلات قطع الغيار في اليمن وتوصيلها اليك . حيث يمكنك اضافه القطع التي تود شرائها بكل مواصفاتها ثم يتم ارسال فاتوره عرض سعر للموافقه عليها ثم يتم توصيلها اليك  تدعم الخدمه تسديد الفاتوره عند الاستلام لكسب ثقه العميل وايضا فحص القطعه قبل الاستلام والتأكد من مطابقه مواصفات الطلب .",
                    color = TextGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }
    )
}

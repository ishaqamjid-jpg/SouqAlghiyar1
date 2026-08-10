package com.isaac.souqalghiyar.presentation.main

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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

fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } else {
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo ?: return false
        @Suppress("DEPRECATION")
        return networkInfo.isConnected
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userId: String,
    viewModel: MainViewModel = hiltViewModel(),
    navigateToRequestParts: (String, String, String, String, String) -> Unit,
    navigateToOrders: (String) -> Unit,
    navigateToNotifications: (String) -> Unit,
    navigateToLogin: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val adsList by viewModel.adsList.collectAsState()
    val brandsList by viewModel.brandsList.collectAsState()
    val isSearchingVin by viewModel.isSearchingVin.collectAsState()
    val isLoadingData by viewModel.isLoadingData.collectAsState()
    val hasPendingOrders by viewModel.hasPendingOrders.collectAsState()
    val hasUnreadNotifications by viewModel.hasUnreadNotifications.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(userId) {
        viewModel.checkPendingOrders(userId)
        viewModel.fetchUserData(userId)
    }

    var brandName by remember { mutableStateOf("") }
    var vehicleModel by remember { mutableStateOf("") }
    var vehicleYear by remember { mutableStateOf("") }
    var manufacture by remember { mutableStateOf("") }
    var vinNumber by remember { mutableStateOf("") }

    var showAboutDialog by remember { mutableStateOf(false) }

    val isRequiredFieldsFilled = brandName.isNotBlank() && vehicleModel.isNotBlank() && vehicleYear.isNotBlank() && manufacture.isNotBlank()

    if (currentUser != null && currentUser!!.number_of_rejections >= 2.0) {
        AlertDialog(
            onDismissRequest = { },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text("تنبيه إيقاف الحساب", color = PrimaryRed, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
            text = { Text("تم ايقاف حسابك بسبب تكرار رفض الفواتير اكثر من مرتين . لتفعيل حسابك يجب فرض رسوم مبلغ وقدره ٢٠٠٠ ريال يمني .\n\nطريقه تسديد الرسوم :\nحواله الى محفظه بجيب الى حساب مشترك رقم 558933 \nوارسال الاشعار وتساب الى الرقم 777979719", color = TextWhite, fontSize = 16.sp, lineHeight = 26.sp) },
            confirmButton = {},
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("سوق الغيار  - لشراء جميع قطع الغيار ", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(onClick = { navigateToNotifications(userId) }) {
                            BadgedBox(
                                badge = { 
                                    if (hasUnreadNotifications) Badge(containerColor = PrimaryRed, modifier = Modifier.offset(x = (-4).dp, y = 4.dp)) { Text("!") } 
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "الإشعارات", tint = TextWhite, modifier = Modifier.size(26.dp))
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = navigateToLogin) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "تسجيل خروج", tint = PrimaryRed, modifier = Modifier.size(26.dp))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = PrimaryRed
                    ),
                    modifier = Modifier.shadow(8.dp)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = SurfaceDark) {
                    NavigationBarItem(
                        selected = false,
                        onClick = { 
                            if (isInternetAvailable(context)) navigateToOrders(userId)
                            else Toast.makeText(context, "لا يوجد اتصال بالإنترنت", Toast.LENGTH_SHORT).show()
                        },
                        icon = { 
                            BadgedBox(badge = { if (hasPendingOrders) Badge(containerColor = PrimaryRed) }) {
                                Icon(Icons.Default.ListAlt, contentDescription = "طلباتي")
                            }
                        },
                        label = { Text("طلباتي") },
                        colors = NavigationBarItemDefaults.colors(
                            unselectedIconColor = TextWhite, unselectedTextColor = TextWhite,
                            selectedIconColor = PrimaryRed, selectedTextColor = PrimaryRed
                        )
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { showAboutDialog = true },
                        icon = { Icon(Icons.Default.Info, contentDescription = "حول النظام") },
                        label = { Text("حول النظام") },
                        colors = NavigationBarItemDefaults.colors(
                            unselectedIconColor = TextWhite, unselectedTextColor = TextWhite,
                            selectedIconColor = PrimaryRed, selectedTextColor = PrimaryRed
                        )
                    )
                }
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

                    if (currentUser != null) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("أهلاً بكم، ", color = TextGray, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            Text(currentUser!!.display_name, color = PrimaryRed, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                            if (isInternetAvailable(context)) {
                                viewModel.searchByVin(
                                    vin = searchVin,
                                    onSuccess = { fetchedBrand, fetchedModel, fetchedYear, fetchedMadeIn ->
                                        brandName = fetchedBrand; vehicleModel = fetchedModel; vehicleYear = fetchedYear; manufacture = fetchedMadeIn
                                        Toast.makeText(context, "تم العثور على بيانات المركبة", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { errorMsg -> Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show() }
                                )
                            } else {
                                Toast.makeText(context, "لا يوجد اتصال بالإنترنت للبحث", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = {
                            if (isInternetAvailable(context)) {
                                navigateToRequestParts(brandName, vehicleModel, vehicleYear, manufacture, vinNumber.ifEmpty { "غير محدد" })
                            } else {
                                Toast.makeText(context, "الرجاء الاتصال بالإنترنت لإرسال الطلب", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp).padding(bottom = 8.dp).shadow(if (isRequiredFieldsFilled) 8.dp else 0.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        enabled = isRequiredFieldsFilled,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed, disabledContainerColor = SurfaceDark)
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
    val madeInOptions = listOf("غير معروف", "مواصفات الولايات المتحدة الأمريكية", "مواصفات خليجي", "اليابان", "المانيا", "كندا")

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
                OutlinedTextField(
                    value = madeIn, 
                    onValueChange = {}, 
                    readOnly = true, 
                    label = { Text("مكان التصنيع *") }, 
                    modifier = Modifier.menuAnchor().fillMaxWidth(), 
                    colors = defaultTextFieldColors, 
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMadeIn) }, 
                    shape = RoundedCornerShape(12.dp)
                )
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
                } else {
                    val isVinValid = vin.trim().length == 17
                    IconButton(
                        onClick = { if (isVinValid) onSearchVin(vin) },
                        enabled = isVinValid
                    ) {
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = "بحث", 
                            tint = if (isVinValid) PrimaryRed else Color.Gray.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedAdsCard(ads: List<Advertisement>) {
    if (ads.isEmpty()) {
        Card(modifier = Modifier.fillMaxWidth().height(140.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryRed, modifier = Modifier.size(28.dp))
            }
        }
        return
    }

    val context = LocalContext.current
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
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .border(1.dp, SurfaceDark, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val currentAd = ads[page]
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        val url = currentAd.target_url
                        if (!url.isNullOrBlank()) {
                            try {
                                var finalUrl = url
                                if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                                    finalUrl = "http://$finalUrl"
                                }
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "لا يمكن فتح الرابط", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            ) {
                AsyncImage(
                    model = currentAd.image_url, 
                    contentDescription = "الإعلان", 
                    contentScale = ContentScale.Crop, 
                    modifier = Modifier.fillMaxSize()
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)))
                )

                Text(
                    text = currentAd.title,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp, start = 16.dp, end = 16.dp),
                    textAlign = TextAlign.Center,
                    color = TextWhite,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                )
            }
        }
    }
}

@Composable
fun AboutSystemDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق", color = PrimaryRed, fontWeight = FontWeight.Bold, fontSize = 16.sp) } },
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
                Text(text = "خدمة العملاء", fontWeight = FontWeight.Bold, color = PrimaryRed, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+967777979719"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "تعذر فتح تطبيق الاتصال", Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Icon(Icons.Default.Call, contentDescription = "اتصال", tint = PrimaryRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("+967-777979719", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable {
                            try {
                                val url = "https://api.whatsapp.com/send?phone=967736373788"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "تعذر فتح تطبيق الواتساب", Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "واتساب", tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("+967-736373788", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ishaq.amjid@gmail.com", color = TextWhite, fontSize = 14.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اليمن - صنعاء", color = TextWhite, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(15.dp))
                Text(text = "حول النظام", fontWeight = FontWeight.ExtraBold, color = PrimaryRed, fontSize = 18.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "تطبيق سوق الغيار هو اول تطبيق في اليمن الذي يوفر شراء قطع غيار لجميع انواع المركبات فى اليمن وتوصيلها اليك ، حيث يمكنك اضافه القطع التي تود شرائها بكل مواصفاتها ثم يتم ارسال فاتوره عرض سعر للموافقه عليها ثم يتم توصيلها اليك تدعم الخدمه تسديد الفاتوره عند الاستلام لكسب ثقه العميل وايضا فحص القطعه قبل الاستلام والتأكد من مطابقه مواصفات الطلب .",
                    color = TextGray, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 22.sp
                )
            }
        }
    )
}
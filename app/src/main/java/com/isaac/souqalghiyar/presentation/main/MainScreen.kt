package com.isaac.souqalghiyar.presentation.main

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
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
import androidx.compose.animation.*

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
    navigateToOrders: (String) -> Unit // تعديل: يجب تمرير الـ userId هنا
) {
    val adsList by viewModel.adsList.collectAsState()
    val brandsList by viewModel.brandsList.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val isLoadingData by viewModel.isLoadingData.collectAsState()
    
    val context = LocalContext.current

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
                        IconButton(onClick = { navigateToOrders(userId) }) { // تعديل: تمرير الـ userId للواجهة
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

// ... [احتفظ ببقية دوال MainScreen كما هي: CarDetailsFields, PhotoPickerBox, AnalyzeButton, AnimatedAdsCard] ...

package com.isaac.souqalghiyar.presentation.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.isaac.souqalghiyar.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Chat

val PrimaryRed = Color(0xFFE53935)
val DarkBackground = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val TextWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFFAAAAAA)

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

// دالة لتنسيق الوقت (مثلاً 05:00)
fun formatTimer(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return String.format("%02d:%02d", min, sec)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    navigateToMain: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val otpCode by viewModel.otpCode.collectAsState()
    val name by viewModel.name.collectAsState()
    val rememberMe by viewModel.rememberMe.collectAsState()

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val activity = context as? Activity

    var showAboutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            val passedId = uiState.userId ?: ""
            if (passedId.isNotEmpty()) {
                navigateToMain(passedId)
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .systemBarsPadding() 
        ) {
            IconButton(
                onClick = { showAboutDialog = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .zIndex(1f) 
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "حول النظام",
                    tint = TextWhite,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 30.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(40.dp))
                
                Image(
                    painter = painterResource(R.drawable.logo3),
                    contentDescription = "الشعار",
                    modifier = Modifier.size(160.dp)
                )
                
                Spacer(Modifier.height(15.dp))
                Text(
                    text = "سوق الغيار",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryRed
                )
                Spacer(Modifier.height(35.dp))

                val customTextFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedLabelColor = PrimaryRed,
                    unfocusedLabelColor = TextWhite,
                    focusedBorderColor = TextWhite,
                    unfocusedBorderColor = TextWhite,
                    focusedContainerColor = SurfaceDark.copy(alpha = 0.5f),
                    unfocusedContainerColor = SurfaceDark.copy(alpha = 0.3f),
                    cursorColor = PrimaryRed
                )

                // ------------------ الخطوة الأولى: إدخال الرقم ------------------
                AnimatedVisibility(
                    visible = uiState.step == LoginStep.ENTER_PHONE,
                    enter = fadeIn(tween(500)),
                    exit = fadeOut(tween(300))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = viewModel::onPhoneChange,
                            label = { Text("رقم الهاتف") },
                            placeholder = { Text("77xxxxxxx", color = TextGray.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (!isInternetAvailable(context)) {
                                        Toast.makeText(context, "يرجى التحقق من اتصالك بالإنترنت", Toast.LENGTH_SHORT).show()
                                    } else if (!uiState.isLoading && activity != null) {
                                        viewModel.startPhoneVerification(activity)
                                    }
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = customTextFieldColors,
                            // إضافة مربع رمز الدولة غير القابل للتعديل
                            leadingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                                ) {
                                    Text(
                                        text = "+967",
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    // فاصل عمودي بسيط بين رمز الدولة ورقم الهاتف
                                    Box(
                                        modifier = Modifier
                                            .height(24.dp)
                                            .width(1.dp)
                                            .background(TextGray.copy(alpha = 0.5f))
                                    )
                                }
                            }
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onRememberMeChange(!rememberMe) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { viewModel.onRememberMeChange(it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = PrimaryRed,
                                    uncheckedColor = TextGray,
                                    checkmarkColor = TextWhite
                                )
                            )
                            Text(
                                text = "تذكرني في المرة القادمة",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(Modifier.height(25.dp))

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (!isInternetAvailable(context)) {
                                    Toast.makeText(context, "يرجى التحقق من اتصالك بالإنترنت", Toast.LENGTH_SHORT).show()
                                } else if (!uiState.isLoading && activity != null) {
                                    viewModel.startPhoneVerification(activity)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(55.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed, contentColor = TextWhite),
                            enabled = !uiState.isLoading,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = TextWhite)
                            } else {
                                Text("تسجيل الدخول / اشتراك", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ------------------ الخطوة الثانية: إدخال رمز الـ SMS ------------------
                AnimatedVisibility(
                    visible = uiState.step == LoginStep.ENTER_CODE,
                    enter = fadeIn(tween(500)),
                    exit = fadeOut(tween(300))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "تم إرسال كود التحقق إلى رقمك",
                            color = TextGray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = viewModel::onOtpCodeChange,
                            label = { Text("رمز التحقق (SMS)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (uiState.timer > 0 && !uiState.isLoading) viewModel.verifyCode()
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = customTextFieldColors,
                            trailingIcon = {
                                Text(
                                    text = formatTimer(uiState.timer),
                                    color = if (uiState.timer > 0) PrimaryRed else TextGray,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            }
                        )

                        Spacer(Modifier.height(25.dp))

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (!isInternetAvailable(context)) {
                                    Toast.makeText(context, "لا يوجد اتصال بالإنترنت", Toast.LENGTH_SHORT).show()
                                } else if (uiState.timer == 0) {
                                    // إذا انتهى الوقت، نعود لخطوة إدخال الرقم لطلب الكود من جديد
                                    viewModel.resetToPhoneStep()
                                } else if (!uiState.isLoading) {
                                    viewModel.verifyCode()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(55.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.timer == 0) SurfaceDark else PrimaryRed,
                                contentColor = TextWhite
                            ),
                            enabled = !uiState.isLoading,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = TextWhite)
                            } else {
                                Text(
                                    text = if (uiState.timer == 0) "إعادة المحاولة" else "تحقق من الرمز",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // ------------------ الخطوة الثالثة: إدخال الاسم (للعملاء الجدد فقط) ------------------
                AnimatedVisibility(
                    visible = uiState.step == LoginStep.ENTER_NAME,
                    enter = fadeIn(tween(500)),
                    exit = fadeOut(tween(300))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "مرحباً بك! يرجى إدخال اسمك لإكمال التسجيل",
                            color = PrimaryRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = viewModel::onNameChange,
                            label = { Text("الاسم الكامل") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (!uiState.isLoading) viewModel.completeRegistration()
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = customTextFieldColors
                        )

                        Spacer(Modifier.height(25.dp))

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (!isInternetAvailable(context)) {
                                    Toast.makeText(context, "لا يوجد اتصال بالإنترنت", Toast.LENGTH_SHORT).show()
                                } else if (!uiState.isLoading) {
                                    viewModel.completeRegistration()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(55.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed, contentColor = TextWhite),
                            enabled = !uiState.isLoading,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = TextWhite)
                            } else {
                                Text("إكمال الدخول", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ------------------ رسائل الخطأ ------------------
                uiState.error?.let {
                    Spacer(Modifier.height(15.dp))
                    Text(
                        text = it,
                        color = PrimaryRed,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (showAboutDialog) {
                AboutSystemDialog(onDismiss = { showAboutDialog = false })
            }
        }
    }
}

@Composable
private fun AboutSystemDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

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

                // أولاً: اتصال برقم الهاتف
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

                // ثانياً: الانتقال إلى الواتساب
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

                Text(
                    text = "حول النظام",
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryRed,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "تطبيق سوق الغيار هو اول تطبيق في اليمن الذي يوفر شراء قطع غيار لجميع انواع المركبات فى اليمن وتوصيلها اليك ، حيث يمكنك اضافه القطع التي تود شرائها بكل مواصفاتها ثم يتم ارسال فاتوره عرض سعر للموافقه عليها ثم يتم توصيلها اليك تدعم الخدمه تسديد الفاتوره عند الاستلام لكسب ثقه العميل وايضا فحص القطعه قبل الاستلام والتأكد من مطابقه مواصفات الطلب .",
                    color = TextGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }
    )
}

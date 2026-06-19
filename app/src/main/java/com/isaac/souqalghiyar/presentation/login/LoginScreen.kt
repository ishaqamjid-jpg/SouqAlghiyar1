package com.isaac.souqalghiyar.presentation.login

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.isaac.souqalghiyar.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.CheckCircle

val PrimaryRed = Color(0xFFE53935)
val DarkBackground = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val TextWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFFAAAAAA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    navigateToMain: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val name by viewModel.name.collectAsState()
    val rememberMe by viewModel.rememberMe.collectAsState()
    val isRegisterMode by viewModel.isRegisterMode.collectAsState()

    val nameFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // حالة التحكم بظهور نافذة حول النظام
    var showAboutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            navigateToMain(phone.ifEmpty { "dev_test_123" })
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            // أيقونة حول النظام في أعلى الركن اليمين
            IconButton(
                onClick = { showAboutDialog = true },
                modifier = Modifier
                    .align(Alignment.TopStart) // يمثل اليمين العلوي في نظام الـ RTL
                    .padding(16.dp)
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
                Surface(
                    modifier = Modifier
                        .size(120.dp)
                        .shadow(15.dp, CircleShape, spotColor = PrimaryRed.copy(alpha = 0.5f)),
                    shape = CircleShape,
                    color = SurfaceDark
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo3),
                        contentDescription = null,
                        modifier = Modifier.padding(15.dp)
                    )
                }
                Spacer(Modifier.height(25.dp))
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

                OutlinedTextField(
                    value = phone,
                    onValueChange = viewModel::onPhoneChange,
                    label = { Text("رقم الهاتف") },
                    placeholder = { Text("77xxxxxxx", color = TextGray.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onNext = { nameFocusRequester.requestFocus() },
                        onDone = {
                            focusManager.clearFocus()
                            if (!uiState.isLoading) viewModel.authenticateUser { navigateToMain(it) }
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = customTextFieldColors
                )

                AnimatedVisibility(visible = isRegisterMode) {
                    Column {
                        Spacer(Modifier.height(15.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = viewModel::onNameChange,
                            label = { Text("الاسم الكامل") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(nameFocusRequester),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (!uiState.isLoading) viewModel.authenticateUser { navigateToMain(it) }
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = customTextFieldColors
                        )
                    }
                }

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
                        viewModel.authenticateUser { navigateToMain(it) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed, contentColor = TextWhite),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = TextWhite)
                    } else {
                        Text(
                            text = if (isRegisterMode) "إنشاء الحساب واشتراك" else "دخول",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                TextButton(onClick = { viewModel.toggleRegisterMode() }) {
                    Text(
                        text = if (isRegisterMode) "لديك حساب بالفعل؟ تسجيل الدخول" else "اشتراك جديد في التطبيق",
                        color = TextGray,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                TextButton(onClick = {
                    val hardcodedDeveloperId = "yP37r324rJZpPDR2xWzL"
                    navigateToMain(hardcodedDeveloperId)
                }) {
                    Text(
                        text = "تخطي الدخول (للمطور فقط)",
                        color = PrimaryRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                }

                uiState.error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = it,
                        color = PrimaryRed,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }

            // عرض نافذة حول النظام عند تفعيل الحالة
            if (showAboutDialog) {
                AboutSystemDialog(onDismiss = { showAboutDialog = false })
            }
        }
    }
}

// --- إضافة النافذة هنا أيضاً ليتعرف عليها ملف الـ Login ---
@Composable
private fun AboutSystemDialog(onDismiss: () -> Unit) {
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
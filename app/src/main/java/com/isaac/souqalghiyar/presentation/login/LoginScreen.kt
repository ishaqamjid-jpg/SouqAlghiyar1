package com.isaac.souqalghiyar.presentation.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.isaac.souqalghiyar.R
import androidx.compose.material3.OutlinedTextFieldDefaults

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

    // التحقق التلقائي من نجاح الدخول لتمرير الـ user_id للشاشة الرئيسية
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            // هنا يمرر المعرف الخاص به للرئيسية
            navigateToMain(phone)
        }
    }

    // إجبار اتجاه الواجهة RTL للتوافق العربي الاحترافي لـ Compose
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0D1B6D), Color(0xFF42A5F5))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 30.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // الشعار الدائري الأنيق الخاص بك
                Surface(
                    modifier = Modifier
                        .size(120.dp)
                        .shadow(10.dp, CircleShape),
                    shape = CircleShape
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo3), // تأكد من وجود الشعار في drawable
                        contentDescription = null,
                        modifier = Modifier.padding(15.dp)
                    )
                }
                Spacer(Modifier.height(25.dp))
                Text(
                    text = "قطع غيار حده",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(35.dp))

                // 1. خانة رقم الهاتف (ثابتة دائماً)
                OutlinedTextField(
                    value = phone,
                    onValueChange = viewModel::onPhoneChange,
                    label = { Text("رقم الهاتف") },
                    placeholder = { Text("77xxxxxxx") },
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        // الخاصية الصحيحة للـ Placeholder في الإصدارات الحديثة:
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f)
                    )
                )

                // 2. خانة الاسم الكامل (تظهر بحركة أنيميشن فقط عند تفعيل وضع الاشتراك الجديد)
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White.copy(0.7f),
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(0.5f)
                            )
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // خيار تذكرني المفضل لديك
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
                            checkedColor = Color.White,
                            uncheckedColor = Color.White.copy(alpha = 0.6f),
                            checkmarkColor = Color(0xFF0D1B6D)
                        )
                    )
                    Text(
                        text = "تذكرني في المرة القادمة",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(25.dp))

                // الزر الديناميكي الأبيض الفخم
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.authenticateUser { navigateToMain(it) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0D1B6D)),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color(0xFF0D1B6D))
                    } else {
                        Text(
                            text = if (isRegisterMode) "إنشاء الحساب واشتراك" else "دخول",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // زر التبديل السفلي التفاعلي للتحول بين الدخول والاشتراك
                TextButton(onClick = { viewModel.toggleRegisterMode() }) {
                    Text(
                        text = if (isRegisterMode) "لديك حساب بالفعل؟ تسجيل الدخول" else "اشتراك جديد في التطبيق",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // عرض الأخطاء باللون الأصفر المميز في تصميمك
                uiState.error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = it,
                        color = Color(0xFFFFEB3B),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
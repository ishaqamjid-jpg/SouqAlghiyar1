package com.isaac.souqalghiyar.presentation.request_parts

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestPartsScreen(
    userId: String,
    vehicleName: String,
    vehicleModel: String,
    picVinNumber: String,
    viewModel: RequestPartsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Observe form fields
    val partName by viewModel.partName.collectAsState()
    val qualityType by viewModel.qualityType.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val description by viewModel.description.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val deliveryLocation by viewModel.deliveryLocation.collectAsState()

    // Dropdown states
    var expandedPart by remember { mutableStateOf(false) }
    var expandedQuality by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            Toast.makeText(context, "تم إرسال الطلب بنجاح!", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("طلب قطع غيار", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0D1B6D),
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = Color(0xFFF5F5F5)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // بطاقة بيانات المركبة (توضيحية للمستخدم)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("المركبة المحددة:", color = Color.Gray, fontSize = 12.sp)
                        Text("$vehicleName - $vehicleModel", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 1. اسم القطعة (قائمة منسدلة + كتابة حرة)
                ExposedDropdownMenuBox(
                    expanded = expandedPart,
                    onExpandedChange = { expandedPart = !expandedPart }
                ) {
                    OutlinedTextField(
                        value = partName,
                        onValueChange = { viewModel.partName.value = it },
                        label = { Text("اسم القطعة") },
                        placeholder = { Text("اكتب أو اختر من القائمة") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0D1B6D))
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPart,
                        onDismissRequest = { expandedPart = false }
                    ) {
                        uiState.categories.filter { it.contains(partName, ignoreCase = true) }.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    viewModel.partName.value = selectionOption
                                    expandedPart = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. الجودة (قائمة منسدلة مغلقة - قراءة فقط)
                ExposedDropdownMenuBox(
                    expanded = expandedQuality,
                    onExpandedChange = { expandedQuality = !expandedQuality }
                ) {
                    OutlinedTextField(
                        value = qualityType,
                        onValueChange = { },
                        readOnly = true, // لا يسمح بالكتابة الحرة هنا
                        label = { Text("جودة القطعة المطلوبة *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedQuality) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0D1B6D))
                    )
                    ExposedDropdownMenu(
                        expanded = expandedQuality,
                        onDismissRequest = { expandedQuality = false }
                    ) {
                        uiState.qualityTypes.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    viewModel.qualityType.value = selectionOption
                                    expandedQuality = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. العدد
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { viewModel.quantity.value = it },
                    label = { Text("العدد *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0D1B6D))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. وصف القطعة
                OutlinedTextField(
                    value = description,
                    onValueChange = { viewModel.description.value = it },
                    label = { Text("وصف إضافي (يمين، يسار، أمامي...)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0D1B6D))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 5. ملاحظات
                OutlinedTextField(
                    value = comments,
                    onValueChange = { viewModel.comments.value = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0D1B6D))
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.LightGray)
                Spacer(modifier = Modifier.height(16.dp))

                // 6. عنوان التوصيل
                OutlinedTextField(
                    value = deliveryLocation,
                    onValueChange = { viewModel.deliveryLocation.value = it },
                    label = { Text("عنوان التوصيل بالكامل *") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0D1B6D))
                )

                Spacer(modifier = Modifier.height(30.dp))

                // زر الإرسال
                Button(
                    onClick = { viewModel.submitOrder(userId, vehicleName, vehicleModel, picVinNumber) },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D1B6D)),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("طلب فاتورة وتسعير", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

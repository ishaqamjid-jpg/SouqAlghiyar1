package com.isaac.souqalghiyar.presentation.request_parts

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.isaac.souqalghiyar.domain.model.OrderItem

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
    val itemsList by viewModel.itemsList.collectAsState()
    val context = LocalContext.current

    // Observe form fields
    val partName by viewModel.partName.collectAsState()
    val qualityType by viewModel.qualityType.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val description by viewModel.description.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val deliveryLocation by viewModel.deliveryLocation.collectAsState()

    var expandedPart by remember { mutableStateOf(false) }
    var expandedQuality by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            Toast.makeText(context, "تم رفع الطلب والفاتورة بنجاح!", Toast.LENGTH_LONG).show()
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
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // رأس بيانات المركبة
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "المركبة: $vehicleName - $vehicleModel",
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D1B6D),
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- قسم 1: بطاقة إدخال القطعة ---
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("بيانات القطعة الجديدة", fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExposedDropdownMenuBox(
                                expanded = expandedPart,
                                onExpandedChange = { expandedPart = !expandedPart },
                                modifier = Modifier.weight(2f)
                            ) {
                                OutlinedTextField(
                                    value = partName,
                                    onValueChange = { viewModel.partName.value = it },
                                    label = { Text("الاسم *") },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedPart,
                                    onDismissRequest = { expandedPart = false }
                                ) {
                                    uiState.categories.filter { it.contains(partName, ignoreCase = true) }.forEach { opt ->
                                        DropdownMenuItem(text = { Text(opt) }, onClick = { viewModel.partName.value = opt; expandedPart = false })
                                    }
                                }
                            }

                            ExposedDropdownMenuBox(
                                expanded = expandedQuality,
                                onExpandedChange = { expandedQuality = !expandedQuality },
                                modifier = Modifier.weight(1.5f)
                            ) {
                                OutlinedTextField(
                                    value = qualityType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("الجودة *") },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedQuality) }
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedQuality,
                                    onDismissRequest = { expandedQuality = false }
                                ) {
                                    uiState.qualityTypes.forEach { opt ->
                                        DropdownMenuItem(text = { Text(opt) }, onClick = { viewModel.qualityType.value = opt; expandedQuality = false })
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = quantity,
                                onValueChange = { viewModel.quantity.value = it },
                                label = { Text("العدد *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = description,
                                onValueChange = { viewModel.description.value = it },
                                label = { Text("وصف إضافي (اختياري)") },
                                modifier = Modifier.weight(2f)
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = comments,
                            onValueChange = { viewModel.comments.value = it },
                            label = { Text("ملاحظات") },
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                        )

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.addItemToTable() },
                            modifier = Modifier.fillMaxWidth().height(45.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("إضافة القطعة للجدول", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- قسم 2: جدول القطع المضافة ---
                if (itemsList.isNotEmpty()) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text("القطع المضافة للطلب (${itemsList.size}):", fontWeight = FontWeight.Bold, color = Color(0xFF0D1B6D))
                        Spacer(Modifier.height(8.dp))

                        // رأس الجدول
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0D1B6D), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الصنف", modifier = Modifier.weight(2f), color = Color.White, fontWeight = FontWeight.Bold)
                            Text("الجودة", modifier = Modifier.weight(1.5f), color = Color.White, fontWeight = FontWeight.Bold)
                            Text("العدد", modifier = Modifier.weight(0.8f), color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text("إزالة", modifier = Modifier.weight(0.7f), color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }

                        // صفوف الجدول
                        itemsList.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .border(0.5.dp, Color.LightGray)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.part_name, modifier = Modifier.weight(2f), fontSize = 14.sp)
                                Text(item.quality_type, modifier = Modifier.weight(1.5f), fontSize = 14.sp, color = Color.Gray)
                                Text(item.quantity.toString(), modifier = Modifier.weight(0.8f), fontSize = 14.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                                IconButton(
                                    onClick = { viewModel.removeItemFromTable(item) },
                                    modifier = Modifier.weight(0.7f).size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                                }
                            }
                        }
                        // إغلاق الجدول من الأسفل
                        Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.White, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)).border(0.5.dp, Color.LightGray))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- قسم 3: عنوان التوصيل والإرسال النهائي ---
                Column(Modifier.padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                        value = deliveryLocation,
                        onValueChange = { viewModel.deliveryLocation.value = it },
                        label = { Text("عنوان التوصيل بالكامل *") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0D1B6D))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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
                            Text("تأكيد وطلب الفاتورة", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

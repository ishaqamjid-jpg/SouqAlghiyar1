package com.isaac.souqalghiyar.presentation.request_parts

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.isaac.souqalghiyar.domain.model.OrderItem

val PrimaryRed = Color(0xFFE53935)
val DarkBackground = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val TextWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFFAAAAAA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestPartsScreen(
    userId: String,
    brandName: String,
    vehicleName: String,
    vehicleModel: String,
    manufacture: String,
    vinNumber: String,
    viewModel: RequestPartsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val itemsList by viewModel.itemsList.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val partName by viewModel.partName.collectAsState()
    val qualityType by viewModel.qualityType.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val description by viewModel.description.collectAsState()
    val comments by viewModel.comments.collectAsState()

    val location by viewModel.location.collectAsState()
    val deliveryLocation by viewModel.deliveryLocation.collectAsState()

    var expandedPart by remember { mutableStateOf(false) }
    var expandedQuality by remember { mutableStateOf(false) }
    var expandedLocation by remember { mutableStateOf(false) }

    val isFormValid = itemsList.isNotEmpty() && location.isNotBlank() && deliveryLocation.isNotBlank()

    val customTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextWhite,
        unfocusedTextColor = TextWhite,
        focusedLabelColor = PrimaryRed,
        unfocusedLabelColor = TextGray,
        focusedBorderColor = PrimaryRed,
        unfocusedBorderColor = SurfaceDark,
        focusedContainerColor = SurfaceDark.copy(alpha = 0.5f),
        unfocusedContainerColor = SurfaceDark.copy(alpha = 0.3f),
        cursorColor = PrimaryRed
    )

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            Toast.makeText(context, "تم رفع الطلب بنجاح!", Toast.LENGTH_LONG).show()
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
                    title = { Text("طلب قطع غيار", fontWeight = FontWeight.ExtraBold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = TextWhite)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = PrimaryRed)
                )
            },
            containerColor = DarkBackground
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceDark,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("بيانات المركبة", fontWeight = FontWeight.Bold, color = PrimaryRed, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("الماركة والموديل: $brandName - $vehicleName $vehicleModel", color = TextWhite)
                        Text("مكان التصنيع: $manufacture", color = TextGray)
                        Text("رقم القعادة: $vinNumber", color = TextGray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).border(1.dp, SurfaceDark, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("بيانات القطعة الجديدة", fontWeight = FontWeight.Bold, color = PrimaryRed)
                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExposedDropdownMenuBox(
                                expanded = expandedPart,
                                onExpandedChange = { expandedPart = !expandedPart },
                                modifier = Modifier.weight(2f)
                            ) {
                                OutlinedTextField(
                                    value = partName,
                                    onValueChange = {
                                        viewModel.partName.value = it
                                        expandedPart = true
                                    },
                                    label = { Text("الاسم *") },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    singleLine = true,
                                    colors = customTextFieldColors,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedPart,
                                    onDismissRequest = { expandedPart = false },
                                    modifier = Modifier.background(SurfaceDark)
                                ) {
                                    uiState.categories.filter { it.contains(partName, ignoreCase = true) }.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt, color = TextWhite) },
                                            onClick = {
                                                viewModel.partName.value = opt
                                                expandedPart = false
                                                focusManager.clearFocus()
                                            }
                                        )
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
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedQuality) },
                                    colors = customTextFieldColors,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedQuality,
                                    onDismissRequest = { expandedQuality = false },
                                    modifier = Modifier.background(SurfaceDark)
                                ) {
                                    uiState.qualityTypes.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt, color = TextWhite) },
                                            onClick = {
                                                viewModel.qualityType.value = opt
                                                expandedQuality = false
                                                focusManager.clearFocus()
                                            }
                                        )
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
                                modifier = Modifier.weight(1f),
                                colors = customTextFieldColors,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = description,
                                onValueChange = { viewModel.description.value = it },
                                label = { Text("وصف إضافي") },
                                modifier = Modifier.weight(2f),
                                colors = customTextFieldColors,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = comments,
                            onValueChange = { viewModel.comments.value = it },
                            label = { Text("ملاحظات") },
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            colors = customTextFieldColors,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.addItemToTable()
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.fillMaxWidth().height(45.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = TextWhite)
                            Spacer(Modifier.width(8.dp))
                            Text("إضافة القطعة للجدول", fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (itemsList.isNotEmpty()) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text("القطع المضافة للطلب (${itemsList.size}): اضغط للتعديل", fontWeight = FontWeight.Bold, color = PrimaryRed)
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الصنف", modifier = Modifier.weight(2f), color = PrimaryRed, fontWeight = FontWeight.Bold)
                            Text("الجودة", modifier = Modifier.weight(1.5f), color = PrimaryRed, fontWeight = FontWeight.Bold)
                            Text("العدد", modifier = Modifier.weight(0.8f), color = PrimaryRed, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text("إزالة", modifier = Modifier.weight(0.7f), color = PrimaryRed, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }

                        itemsList.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceDark)
                                    .border(0.5.dp, Color.DarkGray)
                                    .clickable {
                                        viewModel.editItemFromTable(item)
                                        Toast.makeText(context, "تم سحب القطعة للتعديل", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.part_name, modifier = Modifier.weight(2f), fontSize = 14.sp, color = TextWhite)
                                Text(item.quality_type, modifier = Modifier.weight(1.5f), fontSize = 14.sp, color = TextGray)
                                Text(item.quantity.toString(), modifier = Modifier.weight(0.8f), fontSize = 14.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = TextWhite)
                                IconButton(
                                    onClick = { viewModel.removeItemFromTable(item) },
                                    modifier = Modifier.weight(0.7f).size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = PrimaryRed)
                                }
                            }
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(SurfaceDark, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)).border(0.5.dp, Color.DarkGray))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(Modifier.padding(horizontal = 16.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = expandedLocation,
                        onExpandedChange = { expandedLocation = !expandedLocation },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = location,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("المنطقة / المحافظة *") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLocation) },
                            colors = customTextFieldColors,
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedLocation,
                            onDismissRequest = { expandedLocation = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            uiState.locations.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt, color = TextWhite) },
                                    onClick = {
                                        viewModel.location.value = opt
                                        expandedLocation = false
                                        focusManager.clearFocus()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = deliveryLocation,
                        onValueChange = { viewModel.deliveryLocation.value = it },
                        label = { Text("عنوان التوصيل بالكامل *") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.submitOrder(userId, brandName, vehicleName, vehicleModel, manufacture, vinNumber) },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryRed,
                            disabledContainerColor = SurfaceDark
                        ),
                        enabled = !uiState.isLoading && isFormValid
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = TextWhite, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                "تأكيد وطلب الفاتورة",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFormValid) TextWhite else TextGray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

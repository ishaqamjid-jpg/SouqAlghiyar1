package com.isaac.souqalghiyar.presentation.request_parts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.isaac.souqalghiyar.domain.model.Order
import com.isaac.souqalghiyar.domain.model.OrderItem
import com.isaac.souqalghiyar.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RequestUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val categories: List<String> = emptyList(),
    val qualityTypes: List<String> = emptyList(),
    val locations: List<String> = emptyList()
)

@HiltViewModel
class RequestPartsViewModel @Inject constructor(
    private val repository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestUiState())
    val uiState: StateFlow<RequestUiState> = _uiState.asStateFlow()

    private val _itemsList = MutableStateFlow<List<OrderItem>>(emptyList())
    val itemsList: StateFlow<List<OrderItem>> = _itemsList.asStateFlow()

    val partName = MutableStateFlow("")
    val qualityType = MutableStateFlow("")
    val quantity = MutableStateFlow("1")
    val description = MutableStateFlow("")
    val comments = MutableStateFlow("")
    
    val location = MutableStateFlow("") // اسم المحافظة/المنطقة
    val deliveryLocation = MutableStateFlow("") // تفاصيل العنوان

    init {
        fetchConstants()
    }

    private fun fetchConstants() {
        viewModelScope.launch {
            repository.getCategories().collect { categories ->
                _uiState.value = _uiState.value.copy(categories = categories)
            }
        }
        viewModelScope.launch {
            repository.getQualityTypes().collect { qualities ->
                _uiState.value = _uiState.value.copy(qualityTypes = qualities)
            }
        }
        viewModelScope.launch {
            repository.getLocations().collect { locs ->
                _uiState.value = _uiState.value.copy(locations = locs)
            }
        }
    }

    fun addItemToTable() {
        if (partName.value.isBlank() || qualityType.value.isBlank() || quantity.value.toIntOrNull() == null || quantity.value.toInt() <= 0) {
            _uiState.value = _uiState.value.copy(error = "يرجى تعبئة الحقول الإجبارية للقطعة وإدخال رقم صحيح للعدد.")
            return
        }

        val newItem = OrderItem(
            part_name = partName.value,
            quantity = quantity.value.toInt(),
            quality_type = qualityType.value,
            description = description.value,
            comments = comments.value
        )

        _itemsList.value = _itemsList.value + newItem
        _uiState.value = _uiState.value.copy(error = null)

        // تصفير الحقول للإدخال التالي
        partName.value = ""
        qualityType.value = ""
        quantity.value = "1"
        description.value = ""
        comments.value = ""
    }

    fun removeItemFromTable(item: OrderItem) {
        _itemsList.value = _itemsList.value - item
    }

    fun editItemFromTable(item: OrderItem) {
        partName.value = item.part_name
        qualityType.value = item.quality_type
        quantity.value = item.quantity.toString()
        description.value = item.description
        comments.value = item.comments
        removeItemFromTable(item)
    }

    fun submitOrder(userId: String, brandName: String, vehicleName: String, vehicleModel: String, manufacture: String, vinNumber: String) {
        if (_itemsList.value.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "يرجى إضافة قطعة واحدة على الأقل إلى الجدول.")
            return
        }
        if (location.value.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "يرجى اختيار المنطقة / المحافظة.")
            return
        }
        if (deliveryLocation.value.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "يرجى إدخال تفاصيل عنوان التوصيل.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        val newOrder = Order(
            user_id = userId,
            brand_name = brandName,
            vehicle_name = vehicleName,
            vehicle_model = vehicleModel,
            manufacture = manufacture,
            vin_number = vinNumber.ifEmpty { "غير محدد" },
            location = location.value,
            delivery_location = deliveryLocation.value,
            created_at = Timestamp.now()
        )

        viewModelScope.launch {
            val result = repository.submitOrderWithItems(newOrder, _itemsList.value)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                _itemsList.value = emptyList()
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }
}

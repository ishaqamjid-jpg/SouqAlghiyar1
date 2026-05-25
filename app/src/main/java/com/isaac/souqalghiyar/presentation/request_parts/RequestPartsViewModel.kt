package com.isaac.souqalghiyar.presentation.request_parts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val qualityTypes: List<String> = emptyList()
)

@HiltViewModel
class RequestPartsViewModel @Inject constructor(
    private val repository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestUiState())
    val uiState: StateFlow<RequestUiState> = _uiState.asStateFlow()

    // Form States
    var partName = MutableStateFlow("")
    var qualityType = MutableStateFlow("")
    var quantity = MutableStateFlow("1")
    var description = MutableStateFlow("")
    var comments = MutableStateFlow("")
    var deliveryLocation = MutableStateFlow("")

    init {
        fetchDropdownData()
    }

    private fun fetchDropdownData() {
        viewModelScope.launch {
            repository.getCategories().collect { _uiState.value = _uiState.value.copy(categories = it) }
            repository.getQualityTypes().collect { _uiState.value = _uiState.value.copy(qualityTypes = it) }
        }
    }

    fun submitOrder(userId: String, vehicleName: String, vehicleModel: String, picVinNumber: String) {
        val qty = quantity.value.toIntOrNull() ?: 0

        // Validation
        if (partName.value.isBlank()) { _uiState.value = _uiState.value.copy(error = "يرجى تحديد اسم القطعة"); return }
        if (qualityType.value.isBlank()) { _uiState.value = _uiState.value.copy(error = "يرجى تحديد جودة القطعة"); return }
        if (qty <= 0) { _uiState.value = _uiState.value.copy(error = "يجب أن يكون العدد 1 على الأقل"); return }
        if (deliveryLocation.value.isBlank()) { _uiState.value = _uiState.value.copy(error = "يرجى إدخال عنوان التوصيل"); return }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        val order = Order(
            user_id = userId,
            vehicle_name = vehicleName,
            vehicle_model = vehicleModel,
            pic_vin_number = picVinNumber,
            delivery_location = deliveryLocation.value
        )

        val item = OrderItem(
            part_name = partName.value,
            quantity = qty,
            quality_type = qualityType.value,
            description = description.value,
            comments = comments.value
        )

        viewModelScope.launch {
            repository.submitOrderWithItem(order, item).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true) },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
            )
        }
    }
}

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

    // قائمة القطع المضافة للجدول
    private val _itemsList = MutableStateFlow<List<OrderItem>>(emptyList())
    val itemsList: StateFlow<List<OrderItem>> = _itemsList.asStateFlow()

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

    // دالة إضافة القطعة للجدول المحلي
    fun addItemToTable() {
        val qty = quantity.value.toIntOrNull() ?: 0
        if (partName.value.isBlank() || qualityType.value.isBlank() || qty <= 0) {
            _uiState.value = _uiState.value.copy(error = "يرجى إدخال اسم القطعة، الجودة، والتأكد من العدد")
            return
        }

        val newItem = OrderItem(
            part_name = partName.value,
            quantity = qty,
            quality_type = qualityType.value,
            description = description.value,
            comments = comments.value
        )

        _itemsList.value = _itemsList.value + newItem

        // تفريغ الحقول بعد الإضافة لتسهيل إضافة قطعة جديدة
        partName.value = ""
        quantity.value = "1"
        description.value = ""
        comments.value = ""
        _uiState.value = _uiState.value.copy(error = null) // إزالة الأخطاء
    }

    // دالة إزالة قطعة من الجدول
    fun removeItemFromTable(item: OrderItem) {
        _itemsList.value = _itemsList.value - item
    }

    // الدالة الجديدة: سحب القطعة من الجدول لتعديلها
    fun editItemFromTable(item: OrderItem) {
        partName.value = item.part_name
        qualityType.value = item.quality_type
        quantity.value = item.quantity.toString()
        description.value = item.description
        comments.value = item.comments

        // حذفها من الجدول مؤقتاً لكي يضيفها المستخدم بعد التعديل
        removeItemFromTable(item)
    }

    // دالة إرسال الطلب النهائي لقاعدة البيانات (تستقبل الـ 5 متغيرات الجديدة)
    fun submitOrder(userId: String, make: String, model: String, year: String, madeIn: String, vin: String) {
        if (_itemsList.value.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "الجدول فارغ! يرجى إضافة قطعة واحدة على الأقل.")
            return
        }
        if (deliveryLocation.value.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "يرجى إدخال عنوان التوصيل")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        // تجميع (الموديل + السنة + مكان التصنيع) في حقل واحد ليتوافق مع جدولك في Firebase
        val fullModelDetails = "$model - $year - $madeIn"

        val order = Order(
            user_id = userId,
            vehicle_name = make,
            vehicle_model = fullModelDetails,
            pic_vin_number = vin,
            delivery_location = deliveryLocation.value
        )

        viewModelScope.launch {
            repository.submitOrderWithItems(order, _itemsList.value).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true) },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
            )
        }
    }
}
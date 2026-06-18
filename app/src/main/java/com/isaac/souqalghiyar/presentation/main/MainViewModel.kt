package com.isaac.souqalghiyar.presentation.main

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isaac.souqalghiyar.domain.model.Advertisement
import com.isaac.souqalghiyar.domain.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {

    private val _adsList = MutableStateFlow<List<Advertisement>>(emptyList())
    val adsList: StateFlow<List<Advertisement>> = _adsList.asStateFlow()

    private val _brandsList = MutableStateFlow<List<String>>(emptyList())
    val brandsList: StateFlow<List<String>> = _brandsList.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // حالة جديدة خاصة بزر البحث اليدوي عن رقم الشاصي
    private val _isSearchingVin = MutableStateFlow(false)
    val isSearchingVin: StateFlow<Boolean> = _isSearchingVin.asStateFlow()

    private val _isLoadingData = MutableStateFlow(true)
    val isLoadingData: StateFlow<Boolean> = _isLoadingData.asStateFlow()

    private val _hasPendingOrders = MutableStateFlow(false)
    val hasPendingOrders: StateFlow<Boolean> = _hasPendingOrders.asStateFlow()

    init {
        fetchInitialData()
    }

    private fun fetchInitialData() {
        viewModelScope.launch {
            _isLoadingData.value = true
            launch {
                repository.getActiveAdvertisements().collect { ads -> _adsList.value = ads }
            }
            launch {
                repository.getBrands().collect { brands -> _brandsList.value = brands }
            }
            delay(1500)
            _isLoadingData.value = false
        }
    }

    fun checkPendingOrders(userId: String) {
        viewModelScope.launch {
            // محاكاة أو ربط مع الـ Repository
            _hasPendingOrders.value = true 
        }
    }

    // --- دوال مساعدة وذكية لمعالجة رقم الشاصي (القعادة) ---

    // 1. دالة لتنظيف النص المستخرج من الصورة وتصحيح أخطاء الـ ML Kit الشائعة
    private fun cleanExtractedVin(rawVin: String): String {
        return rawVin.uppercase()
            .replace("O", "0") // الشاصي لا يحتوي حرف O بل رقم 0
            .replace("Q", "0")
            .replace("I", "1") // الشاصي لا يحتوي حرف I بل رقم 1
            .filter { it.isLetterOrDigit() }
    }

    // 2. دالة لمعرفة دولة التصنيع بناءً على أول خانة من رقم الشاصي (نظام WMI العالمي)
    private fun getManufactureCountryFromVin(vin: String): String {
        if (vin.isEmpty()) return "غير معروف"
        return when (vin.first().uppercaseChar()) {
            '1', '4', '5' -> "الولايات المتحدة الأمريكية"
            '2' -> "كندا"
            'J' -> "اليابان"
            'W' -> "المانيا"
            'K' -> "مواصفات خليجي" // افتراضياً للمركبات الكورية غالباً أو يمكن تعديلها
            else -> "غير معروف"
        }
    }

    // استخراج من الصورة
    fun analyzeVinImageFromBitmap(
        bitmap: Bitmap,
        onSuccess: (brand: String, model: String, year: String, madeIn: String, vin: String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                delay(2000) // محاكاة الذكاء الاصطناعي
                
                // افترض أن هذا الرقم هو ما قرأه الذكاء الاصطناعي
                val rawVin = "1NXBR32E23B123456" 
                val cleanVin = cleanExtractedVin(rawVin)
                
                // تحديد الدولة تلقائياً
                val autoCountry = getManufactureCountryFromVin(cleanVin)

                // القيم الأخرى مستخرجة وهمياً كمثال
                onSuccess("تويوتا", "كورولا", "2022", autoCountry, cleanVin)
            } catch (e: Exception) {
                onError("فشل في تحليل الصورة: ${e.message}")
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    // البحث اليدوي برقم الشاصي
    fun searchByVin(
        vin: String,
        onSuccess: (brand: String, model: String, year: String, madeIn: String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSearchingVin.value = true
            try {
                delay(1500) // محاكاة البحث في قاعدة بيانات أو API
                
                if (vin.length < 10) throw Exception("رقم الشاصي قصير جداً للبحث")
                
                val cleanVin = cleanExtractedVin(vin)
                val autoCountry = getManufactureCountryFromVin(cleanVin)

                // إرجاع بيانات وهمية ناجحة بناءً على البحث
                onSuccess("تويوتا", "كامري", "2023", autoCountry)
            } catch (e: Exception) {
                onError(e.message ?: "حدث خطأ أثناء البحث")
            } finally {
                _isSearchingVin.value = false
            }
        }
    }
}

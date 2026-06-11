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

    // متغير للتحكم في شاشة الانتظار عند فتح التطبيق
    private val _isLoadingData = MutableStateFlow(true)
    val isLoadingData: StateFlow<Boolean> = _isLoadingData.asStateFlow()

    init {
        fetchInitialData()
    }

    private fun fetchInitialData() {
        viewModelScope.launch {
            _isLoadingData.value = true
            
            // تشغيل جلب البيانات بالتوازي
            launch {
                repository.getActiveAdvertisements().collect { ads ->
                    _adsList.value = ads
                }
            }
            launch {
                repository.getBrands().collect { brands ->
                    _brandsList.value = brands
                }
            }
            
            // إعطاء وقت قصير للتأكد من جلب البيانات قبل إخفاء مؤشر التحميل
            delay(1500)
            _isLoadingData.value = false
        }
    }

    fun analyzeVinImageFromBitmap(
        bitmap: Bitmap,
        onSuccess: (brand: String, model: String, year: String, madeIn: String, vin: String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                // محاكاة الاتصال بـ ML Kit و استخراج البيانات
                delay(2000)
                
                // البيانات المستخرجة وهمياً كمثال
                val extractedBrand = "تويوتا"
                val extractedModel = "كورولا"
                val extractedYear = "2022"
                val extractedMadeIn = "أمريكي"
                val extractedVin = "1NXBR32E23B123456"

                onSuccess(extractedBrand, extractedModel, extractedYear, extractedMadeIn, extractedVin)
            } catch (e: Exception) {
                onError("فشل في تحليل الصورة: ${e.message}")
            } finally {
                _isAnalyzing.value = false
            }
        }
    }
}

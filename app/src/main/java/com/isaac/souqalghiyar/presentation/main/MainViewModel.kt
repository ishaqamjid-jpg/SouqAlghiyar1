package com.isaac.souqalghiyar.presentation.main

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.isaac.souqalghiyar.domain.model.Advertisement
import com.isaac.souqalghiyar.domain.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.android.gms.tasks.Task

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {

    private val _adsList = MutableStateFlow<List<Advertisement>>(emptyList())
    val adsList: StateFlow<List<Advertisement>> = _adsList.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    // مٌعرف مكتبة التعرف على النصوص اللاتينية (الإنجليزية والأرقام)
    private val textRecognizer: com.google.mlkit.vision.text.TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    init {
        fetchAdvertisements()
    }

    private fun fetchAdvertisements() {
        viewModelScope.launch {
            repository.getAdvertisements()
                .catch { e -> Log.e("MainViewModel", "Error fetching ads: ${e.message}") }
                .collect { ads -> _adsList.value = ads }
        }
    }

    /**
     * دالة فعلية تستخدم ML Kit لاستخراج النص (VIN و Made In) من صورة Bitmap
     */
    fun analyzeVinImageFromBitmap(
        bitmap: Bitmap,
        onSuccess: (make: String, model: String, year: String, madeIn: String, vin: String) -> Unit,
        onError: (String) -> Unit
    ) {
        _isAnalyzing.value = true
        _analysisError.value = null

        // 1. تحويل الـ Bitmap إلى InputImage الخاص بـ ML Kit
        val image = InputImage.fromBitmap(bitmap, 0)

        // 2. تمرير الصورة للمكتبة لقراءة النصوص
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                // النص الكامل المستخرج من الصورة
                val fullText = visionText.text
                Log.d("MLKit", "Extracted Text: \n$fullText")

                // 3. تحليل النص المستخرج (Parsing)
                var extractedVin = ""
                var extractedMadeIn = ""

                // البحث عن كلمة Made in USA كمثال
                if (fullText.contains("MADE IN U.S.A.", ignoreCase = true) ||
                    fullText.contains("MADE IN USA", ignoreCase = true)) {
                    extractedMadeIn = "U.S.A."
                } else if (fullText.contains("JAPAN", ignoreCase = true)) {
                    extractedMadeIn = "Japan"
                }

                // البحث عن رقم القعادة (VIN) - عادة يتكون من 17 حرف ورقم
                // نستخدم التعبير النمطي (Regex) للبحث عن أي تسلسل من 17 حرف/رقم إنجليزي
                val vinRegex = Regex("[A-HJ-NPR-Z0-9]{17}")
                val matchResult = vinRegex.find(fullText.replace(" ", "")) // إزالة الفراغات لتسهيل البحث

                if (matchResult != null) {
                    extractedVin = matchResult.value
                }

                _isAnalyzing.value = false

                // في هذه المرحلة، إذا تم استخراج الـ VIN بنجاح، يمكنك تمريره إلى
                // API (مثل NHTSA) لجلب الماركة والموديل.
                // مؤقتاً، سنضع بيانات تقريبية إذا وجدنا الـ VIN
                if (extractedVin.isNotEmpty()) {
                    onSuccess("Toyota", "Corolla", "2022", extractedMadeIn, extractedVin)
                } else {
                    onError("لم نتمكن من العثور على رقم القعادة (17 خانة) في الصورة. يرجى التأكد من وضوح الصورة.")
                }
            }
            .addOnFailureListener { e ->
                _isAnalyzing.value = false
                _analysisError.value = "فشل في تحليل الصورة: ${e.localizedMessage}"
                onError("حدث خطأ أثناء تحليل الصورة.")
                Log.e("MLKit", "Error recognizing text", e)
            }
    }
}
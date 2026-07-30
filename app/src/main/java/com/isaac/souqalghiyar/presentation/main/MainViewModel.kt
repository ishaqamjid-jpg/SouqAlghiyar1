package com.isaac.souqalghiyar.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isaac.souqalghiyar.domain.model.Advertisement
import com.isaac.souqalghiyar.domain.model.users
import com.isaac.souqalghiyar.domain.repository.MainRepository
import com.isaac.souqalghiyar.domain.repository.UserRepository
import com.isaac.souqalghiyar.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepository,
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<users?>(null)
    val currentUser: StateFlow<users?> = _currentUser.asStateFlow()

    private val _adsList = MutableStateFlow<List<Advertisement>>(emptyList())
    val adsList: StateFlow<List<Advertisement>> = _adsList.asStateFlow()

    private val _brandsList = MutableStateFlow<List<String>>(emptyList())
    val brandsList: StateFlow<List<String>> = _brandsList.asStateFlow()

    private val _isSearchingVin = MutableStateFlow(false)
    val isSearchingVin: StateFlow<Boolean> = _isSearchingVin.asStateFlow()

    private val _isLoadingData = MutableStateFlow(true)
    val isLoadingData: StateFlow<Boolean> = _isLoadingData.asStateFlow()

    private val _hasPendingOrders = MutableStateFlow(false)
    val hasPendingOrders: StateFlow<Boolean> = _hasPendingOrders.asStateFlow()

    private val _hasUnreadNotifications = MutableStateFlow(false)
    val hasUnreadNotifications: StateFlow<Boolean> = _hasUnreadNotifications.asStateFlow()

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

    fun fetchUserData(userId: String) {
        viewModelScope.launch {
            userRepository.getUserData(userId).collect { user ->
                _currentUser.value = user
            }
        }
        
        viewModelScope.launch {
            notificationRepository.getUserNotifications(userId).collect { alarms ->
                _hasUnreadNotifications.value = alarms.any { !it.isRead }
            }
        }
    }

    fun checkPendingOrders(userId: String) {
        viewModelScope.launch {
            _hasPendingOrders.value = true 
        }
    }

    // تنظيف رقم الشاصي المدخل
    private fun cleanExtractedVin(rawVin: String): String {
        var cleanText = rawVin.replace(Regex("\\s+"), "").uppercase()
        cleanText = cleanText.replace("O", "0")
            .replace("Q", "0")
            .replace("I", "1")
        
        val vinRegex = Regex("[A-HJ-NPR-Z0-9]{17}")
        val match = vinRegex.find(cleanText)
        return match?.value ?: cleanText.filter { it.isLetterOrDigit() }
    }

    private fun getManufactureCountryFromVin(vin: String): String {
        if (vin.isEmpty()) return "غير معروف"
        return when (vin.first().uppercaseChar()) {
            '1', '4', '5' -> "الولايات المتحدة الأمريكية"
            '2' -> "كندا"
            '3' -> "المكسيك"
            'J' -> "اليابان"
            'K' -> "كوريا الجنوبية"
            'S' -> "المملكة المتحدة (بريطانيا)"
            'V' -> "فرنسا / إسبانيا"
            'T' -> "سويسرا"
            'W' -> "المانيا"
            'Z' -> "إيطاليا"
            'L' -> "الصين"
            else -> "غير معروف / مواصفات أخرى"
        }
    }

    private fun getBrandFromVin(vin: String): String {
        if (vin.length < 3) return "غير محدد"
        val wmi = vin.substring(0, 3).uppercase()
        return when {
            wmi.startsWith("1G") -> "شيفروليه / جي إم سي"
            wmi.startsWith("1F") -> "فورد"
            wmi.startsWith("1N") -> "نيسان"
            wmi.startsWith("JT") -> "تويوتا"
            wmi.startsWith("JM") -> "مازدا"
            wmi.startsWith("JH") -> "هوندا"
            wmi.startsWith("KM") -> "هيونداي"
            wmi.startsWith("WA") -> "أودي"
            wmi.startsWith("WB") -> "بي إم دبليو"
            wmi.startsWith("WD") -> "مرسيدس بنز"
            else -> "غير محدد"
        }
    }

    private suspend fun fetchCarDetailsFromApi(vin: String): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://vpic.nhtsa.dot.gov/api/vehicles/decodevinvalues/$vin?format=json")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonObject = JSONObject(response)
                val resultsArray = jsonObject.optJSONArray("Results")
                
                if (resultsArray != null && resultsArray.length() > 0) {
                    val carData = resultsArray.getJSONObject(0)
                    val make = carData.optString("Make", "")
                    val model = carData.optString("Model", "")
                    val year = carData.optString("ModelYear", "")
                    
                    return@withContext mapOf("brand" to make, "model" to model, "year" to year)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext emptyMap()
    }

    fun searchByVin(
        vin: String,
        onSuccess: (brand: String, model: String, year: String, madeIn: String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSearchingVin.value = true
            try {
                val cleanVin = cleanExtractedVin(vin)
                if (cleanVin.length < 10) throw Exception("رقم الشاصي قصير جداً للبحث")
                
                // البحث في API الحقيقي
                val apiData = fetchCarDetailsFromApi(cleanVin)
                val autoCountry = getManufactureCountryFromVin(cleanVin)
                
                val finalBrand = apiData["brand"].takeIf { !it.isNullOrEmpty() } ?: getBrandFromVin(cleanVin)
                val finalModel = apiData["model"] ?: ""
                val finalYear = apiData["year"] ?: ""

                onSuccess(finalBrand, finalModel, finalYear, autoCountry)
            } catch (e: Exception) {
                onError(e.message ?: "حدث خطأ أثناء البحث")
            } finally {
                _isSearchingVin.value = false
            }
        }
    }
}

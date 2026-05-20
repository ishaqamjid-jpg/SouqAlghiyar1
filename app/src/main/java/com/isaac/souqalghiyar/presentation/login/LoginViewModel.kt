package com.isaac.souqalghiyar.presentation.login

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// دمج الـ UiState في نفس الملف لتقليل عدد الملفات
data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val sharedPref = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _rememberMe = MutableStateFlow(true) // مفعل تلقائياً كخيار افتراضي ذكي
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

    private val _isRegisterMode = MutableStateFlow(false) // يحدد هل الواجهة دخول أم اشتراك
    val isRegisterMode: StateFlow<Boolean> = _isRegisterMode.asStateFlow()

    fun onPhoneChange(phone: String) { _phone.value = phone }
    fun onNameChange(name: String) { _name.value = name }
    fun onRememberMeChange(checked: Boolean) { _rememberMe.value = checked }

    fun toggleRegisterMode() {
        _isRegisterMode.value = !_isRegisterMode.value
        _uiState.value = _uiState.value.copy(error = null) // تصفير الأخطاء عند الانتقال
    }

    init {
        // فحص تلقائي: إذا كان العميل مسجل ومفعل تذكرني يدخل فوراً للرئيسية
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)
        if (isLoggedIn) {
            _uiState.value = LoginUiState(isSuccess = true)
        }
    }

    fun authenticateUser(onSuccess: (String) -> Unit) {
        val currentPhone = _phone.value.trim()
        val currentName = _name.value.trim()

        if (currentPhone.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "يرجى إدخال رقم الهاتف")
            return
        }

        if (_isRegisterMode.value && currentName.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "يرجى إدخال الاسم لإتمام الاشتراك")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        if (_isRegisterMode.value) {
            runRegister(currentPhone, currentName, onSuccess)
        } else {
            runLogin(currentPhone, onSuccess)
        }
    }

    private fun runRegister(phone: String, name: String, onSuccess: (String) -> Unit) {
        db.collection("Users")
            .whereEqualTo("phone_number", phone)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "هذا الرقم مسجل مسبقاً، قم بتسجيل الدخول")
                } else {
                    val userId = db.collection("Users").document().id
                    val mockFcmToken = "mock_fcm_${System.currentTimeMillis()}"

                    val userMap = hashMapOf(
                        "user_id" to userId,
                        "phone_number" to phone,
                        "display_name" to name,
                        "created_at" to FieldValue.serverTimestamp(),
                        "status" to "active",
                        "fcm_token" to mockFcmToken
                    )

                    db.collection("Users").document(userId).set(userMap)
                        .addOnSuccessListener {
                            if (_rememberMe.value) saveSessionLocally(userId, name, phone)
                            _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                            onSuccess(userId)
                        }
                        .addOnFailureListener { e ->
                            _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                        }
                }
            }
    }

    private fun runLogin(phone: String, onSuccess: (String) -> Unit) {
        db.collection("Users")
            .whereEqualTo("phone_number", phone)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "الحساب غير موجود، يرجى اختيار اشتراك جديد")
                } else {
                    val document = documents.documents[0]
                    val status = document.getString("status") ?: "active"
                    val userId = document.getString("user_id") ?: ""
                    val name = document.getString("display_name") ?: ""

                    if (status == "banned") {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = "عذراً، هذا الحساب محظور من النظام")
                        return@addOnSuccessListener
                    }

                    // تحديث الـ FCM Token للجهاز الجديد المتصل
                    val mockFcmToken = "mock_fcm_updated_${System.currentTimeMillis()}"
                    db.collection("Users").document(userId).update("fcm_token", mockFcmToken)

                    if (_rememberMe.value) saveSessionLocally(userId, name, phone)
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                    onSuccess(userId)
                }
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
    }

    private fun saveSessionLocally(userId: String, name: String, phone: String) {
        sharedPref.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_id", userId)
            putString("user_name", name)
            putString("user_phone", phone)
            apply()
        }
    }
}
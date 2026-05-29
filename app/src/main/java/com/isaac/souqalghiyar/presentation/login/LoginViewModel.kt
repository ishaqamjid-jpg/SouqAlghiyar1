package com.isaac.souqalghiyar.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isaac.souqalghiyar.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _rememberMe = MutableStateFlow(true)
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

    private val _isRegisterMode = MutableStateFlow(false)
    val isRegisterMode: StateFlow<Boolean> = _isRegisterMode.asStateFlow()

    fun onPhoneChange(phone: String) { _phone.value = phone }
    fun onNameChange(name: String) { _name.value = name }
    fun onRememberMeChange(checked: Boolean) { _rememberMe.value = checked }

    fun toggleRegisterMode() {
        _isRegisterMode.value = !_isRegisterMode.value
        _uiState.value = _uiState.value.copy(error = null)
    }

    init {
        if (authRepository.checkIsLoggedIn()) {
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

        viewModelScope.launch {
            val result = authRepository.authenticateUser(currentPhone, currentName, _isRegisterMode.value)
            result.fold(
                onSuccess = { userId ->
                    if (_rememberMe.value) {
                        authRepository.saveSessionLocally(userId, currentName, currentPhone)
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                    onSuccess(userId)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.message)
                }
            )
        }
    }

    // --- تم تعديل هذه الدالة لتطابق بيانات الـ Firebase الخاصة بك ---
    fun testLogin(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            kotlinx.coroutines.delay(500)

            // البيانات المأخوذة من صورة قاعدة البيانات المرفقة
            val testUserId = "test_user_123"
            val testUserName = "امجد"
            val testUserPhone = "+967770000000"

            // حفظ الجلسة لكي يعمل التطبيق وكأن "أمجد" هو من سجل الدخول
            authRepository.saveSessionLocally(testUserId, testUserName, testUserPhone)

            _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            onSuccess(testUserId)
        }
    }
}
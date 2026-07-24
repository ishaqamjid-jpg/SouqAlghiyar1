package com.isaac.souqalghiyar.presentation.login

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.isaac.souqalghiyar.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class LoginStep {
    ENTER_PHONE, ENTER_CODE, ENTER_NAME
}

data class LoginUiState(
    val step: LoginStep = LoginStep.ENTER_PHONE,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val userId: String? = null,
    val timer: Int = 0,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth // يجب إضافتها في AppModule
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _rememberMe = MutableStateFlow(true)
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

    private var storedVerificationId: String = ""
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var timerJob: Job? = null

    init {
        if (authRepository.checkIsLoggedIn()) {
            val savedUserId = authRepository.getUserId()
            if (savedUserId != null) {
                _uiState.value = _uiState.value.copy(isSuccess = true, userId = savedUserId)
            }
        }
    }

    fun onPhoneChange(phone: String) { _phone.value = phone }
    fun onOtpCodeChange(code: String) { _otpCode.value = code }
    fun onNameChange(name: String) { _name.value = name }
    fun onRememberMeChange(checked: Boolean) { _rememberMe.value = checked }

    // بدء طلب الرمز (OTP)
    fun startPhoneVerification(activity: Activity) {
        val currentPhone = _phone.value.trim()
        if (currentPhone.isEmpty() || currentPhone.length < 9) {
            _uiState.value = _uiState.value.copy(error = "يرجى إدخال رقم هاتف صحيح مع رمز الدولة (مثل: +967...)")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(currentPhone)
            .setTimeout(60L, TimeUnit.SECONDS) // Firebase timeout
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (resendToken != null) {
            options.setForceResendingToken(resendToken!!)
        }

        PhoneAuthProvider.verifyPhoneNumber(options.build())
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // يتم استدعاؤها أحياناً إذا تم التحقق التلقائي للرمز من السيم كارد
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "فشل التحقق: ${e.message}")
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            storedVerificationId = verificationId
            resendToken = token
            _uiState.value = _uiState.value.copy(isLoading = false, step = LoginStep.ENTER_CODE, error = null)
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(timer = 300) // 5 دقائق = 300 ثانية
        timerJob = viewModelScope.launch {
            while (_uiState.value.timer > 0) {
                delay(1000)
                _uiState.value = _uiState.value.copy(timer = _uiState.value.timer - 1)
            }
        }
    }

    // التحقق من الرمز الذي أدخله المستخدم
    fun verifyCode() {
        val currentCode = _otpCode.value.trim()
        if (currentCode.isEmpty() || currentCode.length < 6) {
            _uiState.value = _uiState.value.copy(error = "يرجى إدخال رمز صحيح (6 أرقام)")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val credential = PhoneAuthProvider.getCredential(storedVerificationId, currentCode)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            try {
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                val user = authResult.user
                if (user != null) {
                    val uid = user.uid
                    checkIfUserExistsAndProceed(uid, user.phoneNumber ?: _phone.value)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "الرمز غير صحيح، يرجى المحاولة مرة أخرى.")
            }
        }
    }

    private suspend fun checkIfUserExistsAndProceed(uid: String, phoneNumber: String) {
        val existsResult = authRepository.checkUserExistsAndGetName(uid)
        
        existsResult.fold(
            onSuccess = { savedName ->
                if (!savedName.isNullOrEmpty()) {
                    // المستخدم مسجل مسبقاً، الدخول مباشرة للرئيسية
                    if (_rememberMe.value) {
                        authRepository.saveSessionLocally(uid, savedName, phoneNumber)
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true, userId = uid)
                } else {
                    // عميل جديد، نطلب منه الاسم
                    _uiState.value = _uiState.value.copy(isLoading = false, step = LoginStep.ENTER_NAME, userId = uid)
                }
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "حدث خطأ أثناء فحص البيانات.")
            }
        )
    }

    // حفظ الاسم النهائي للعميل الجديد والتوجه للرئيسية
    fun completeRegistration() {
        val currentName = _name.value.trim()
        val uid = _uiState.value.userId ?: return
        val currentPhone = firebaseAuth.currentUser?.phoneNumber ?: _phone.value

        if (currentName.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "يرجى كتابة اسمك لإكمال التسجيل")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val saveResult = authRepository.saveUserData(uid, currentPhone, currentName)
            saveResult.fold(
                onSuccess = {
                    if (_rememberMe.value) {
                        authRepository.saveSessionLocally(uid, currentName, currentPhone)
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun resetToPhoneStep() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(step = LoginStep.ENTER_PHONE, timer = 0, error = null)
        _otpCode.value = ""
    }
}

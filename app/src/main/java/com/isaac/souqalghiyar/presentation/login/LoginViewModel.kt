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
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    // متغيرات الاسم الثلاثي
    private val _firstName = MutableStateFlow("")
    val firstName: StateFlow<String> = _firstName.asStateFlow()

    private val _fatherName = MutableStateFlow("")
    val fatherName: StateFlow<String> = _fatherName.asStateFlow()

    private val _lastName = MutableStateFlow("")
    val lastName: StateFlow<String> = _lastName.asStateFlow()

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

    fun onPhoneChange(phone: String) {
        if (phone.all { it.isDigit() } && phone.length <= 9) {
            _phone.value = phone
        }
    }

    fun onOtpCodeChange(code: String) {
        if(code.all { it.isDigit() } && code.length <= 6) {
            _otpCode.value = code
        }
    }
    
    // دوال تحديث الاسم
    fun onFirstNameChange(name: String) { _firstName.value = name }
    fun onFatherNameChange(name: String) { _fatherName.value = name }
    fun onLastNameChange(name: String) { _lastName.value = name }
    
    fun onRememberMeChange(checked: Boolean) { _rememberMe.value = checked }

    fun startPhoneVerification(activity: Activity) {
        val currentPhone = _phone.value.trim()

        if (currentPhone.isEmpty() || currentPhone.length < 9) {
            _uiState.value = _uiState.value.copy(error = "يرجى إدخال رقم هاتف صحيح (9 أرقام)")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val fullPhoneNumber = "+967$currentPhone"

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(fullPhoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (resendToken != null) {
            options.setForceResendingToken(resendToken!!)
        }

        PhoneAuthProvider.verifyPhoneNumber(options.build())
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            val errorMessage = if (e.message?.contains("blocked all requests") == true || e.message?.contains("too_many_requests") == true) {
                "تم حظر الطلبات من هذا الجهاز مؤقتاً بسبب المحاولات المتكررة. يرجى المحاولة لاحقاً."
            } else {
                "فشل التحقق: ${e.message}"
            }
            _uiState.value = _uiState.value.copy(isLoading = false, error = errorMessage)
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
        _uiState.value = _uiState.value.copy(timer = 300)
        timerJob = viewModelScope.launch {
            while (_uiState.value.timer > 0) {
                delay(1000)
                _uiState.value = _uiState.value.copy(timer = _uiState.value.timer - 1)
            }
        }
    }

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
                    val fullPhoneToSave = user.phoneNumber ?: "+967${_phone.value}"
                    checkIfUserExistsAndProceed(uid, fullPhoneToSave)
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
                    // في حال كان المستخدم قديماً، يتم استخدام اسمه القديم المحفوظ والدخول مباشرة
                    if (_rememberMe.value) {
                        authRepository.saveSessionLocally(uid, savedName, phoneNumber)
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true, userId = uid)
                } else {
                    // في حال كان المستخدم جديداً، يتم توجيهه لإدخال الاسم الثلاثي
                    _uiState.value = _uiState.value.copy(isLoading = false, step = LoginStep.ENTER_NAME, userId = uid)
                }
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "حدث خطأ أثناء فحص البيانات.")
            }
        )
    }

    fun completeRegistration() {
        val fName = _firstName.value.trim()
        val mName = _fatherName.value.trim()
        val lName = _lastName.value.trim()
        
        val uid = _uiState.value.userId ?: return
        val currentPhone = firebaseAuth.currentUser?.phoneNumber ?: "+967${_phone.value}"

        if (fName.isEmpty() || mName.isEmpty() || lName.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "يرجى تعبئة جميع الخانات (الاسم الأول، اسم الأب، اللقب)")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        // دمج الأسماء في متغير واحد
        val fullDisplayName = "$fName $mName $lName"

        viewModelScope.launch {
            val saveResult = authRepository.saveUserData(uid, currentPhone, fullDisplayName)
            saveResult.fold(
                onSuccess = {
                    if (_rememberMe.value) {
                        authRepository.saveSessionLocally(uid, fullDisplayName, currentPhone)
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

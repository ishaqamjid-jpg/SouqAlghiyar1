package com.isaac.souqalghiyar.data.repository

import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.isaac.souqalghiyar.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val sharedPref: SharedPreferences
) : AuthRepository {

    override suspend fun authenticateUser(phone: String, name: String, isRegisterMode: Boolean): Result<String> {
        // كود مؤقت للاختبار (Mock) لضمان فصل المنطق عن الواجهة
        delay(1000)
        return if (phone == "777777777") {
            Result.success("dev_user_123")
        } else if (isRegisterMode) {
            Result.success("test_user_${System.currentTimeMillis()}")
        } else {
            Result.failure(Exception("رقم غير مسجل. للطور التجريبي استخدم 777777777"))
        }
    }

    override fun saveSessionLocally(userId: String, name: String, phone: String) {
        sharedPref.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_id", userId)
            putString("user_name", name)
            putString("user_phone", phone)
            apply()
        }
    }

    override fun checkIsLoggedIn(): Boolean {
        return sharedPref.getBoolean("is_logged_in", false)
    }
}

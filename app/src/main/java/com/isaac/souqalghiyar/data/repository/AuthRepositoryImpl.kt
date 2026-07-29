package com.isaac.souqalghiyar.data.repository

import android.content.SharedPreferences
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.isaac.souqalghiyar.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val sharedPref: SharedPreferences
) : AuthRepository {

    override suspend fun checkUserExistsAndGetName(userId: String): Result<String?> {
        return try {
            val document = db.collection("users").document(userId).get().await()
            if (document.exists()) {
                val name = document.getString("display_name")
                Result.success(name)
            } else {
                Result.success(null) // عميل جديد
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveUserData(userId: String, phone: String, name: String): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(userId)
            
            // نتحقق أولاً: هل العميل موجود مسبقاً؟
            val document = userRef.get().await()

            if (!document.exists()) {
                // 1. العميل جديد تماماً: نقوم بتعبئة الجدول بالبيانات الافتراضية كاملة
                val newUserMap = hashMapOf(
                    "user_id" to userId,                 // تم نسخ المعرف هنا
                    "phone_number" to phone,
                    "display_name" to name,
                    "fcm_token" to "",                   // فارغ مؤقتاً (الـ MainActivity سيقوم بتحديثه)
                    "status" to "active",                // مفعل افتراضياً
                    "number_of_rejections" to 0.0,       // صفر افتراضياً
                    "created_at" to Timestamp.now()
                )
                userRef.set(newUserMap).await()
            } else {
                // 2. العميل مسجل مسبقاً: نقوم بتحديث الاسم والرقم فقط حتى لا نمسح عدد مرات الرفض أو الحظر
                val updatesMap = hashMapOf<String, Any>(
                    "display_name" to name,
                    "phone_number" to phone
                )
                userRef.update(updatesMap).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun saveSessionLocally(userId: String, name: String, phone: String) {
        sharedPref.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_id", userId)
            if (name.isNotEmpty()) putString("user_name", name)
            putString("user_phone", phone)
            apply()
        }
    }

    override fun checkIsLoggedIn(): Boolean {
        return sharedPref.getBoolean("is_logged_in", false)
    }

    override fun getUserId(): String? {
        return sharedPref.getString("user_id", null)
    }
}

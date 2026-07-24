package com.isaac.souqalghiyar.data.repository

import android.content.SharedPreferences
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.isaac.souqalghiyar.domain.model.users
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
                Result.success(null) // العميل جديد
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveUserData(userId: String, phone: String, name: String): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(userId)
            
            val userData = hashMapOf(
                "user_id" to userId,
                "phone_number" to phone,
                "display_name" to name,
                "status" to "active",
                "created_at" to Timestamp.now()
            )
            
            // نستخدم merge حتى لا نمسح البيانات الأخرى (مثل عدد المرات المرفوضة) إذا كان المستخدم موجوداً
            userRef.set(userData, SetOptions.merge()).await()
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

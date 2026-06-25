package com.isaac.souqalghiyar.data.repository

import android.content.SharedPreferences
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.isaac.souqalghiyar.domain.model.users
import com.isaac.souqalghiyar.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val sharedPref: SharedPreferences
) : AuthRepository {

    override suspend fun authenticateUser(phone: String, name: String, isRegisterMode: Boolean): Result<String> {
        return try {
            val usersRef = db.collection("users")
            val querySnapshot = usersRef.whereEqualTo("phone_number", phone).get().await()

            if (isRegisterMode) {
                if (!querySnapshot.isEmpty) {
                    return Result.failure(Exception("رقم الهاتف مسجل مسبقاً. يرجى تسجيل الدخول."))
                }
                val newUserRef = usersRef.document()
                val newUser = users(
                    user_id = newUserRef.id,
                    phone_number = phone,
                    display_name = name,
                    created_at = Timestamp.now()
                )
                newUserRef.set(newUser).await()
                Result.success(newUserRef.id)
            } else {
                if (querySnapshot.isEmpty) {
                    if (phone == "777777777") { // وصول استثنائي للمطور
                        return Result.success("dev_user_123")
                    }
                    return Result.failure(Exception("رقم الهاتف غير مسجل. يرجى إنشاء حساب جديد."))
                }
                val userDoc = querySnapshot.documents.first()
                Result.success(userDoc.id)
            }
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
}

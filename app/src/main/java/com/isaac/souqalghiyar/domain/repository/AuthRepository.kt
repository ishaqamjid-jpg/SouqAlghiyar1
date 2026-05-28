package com.isaac.souqalghiyar.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    // دالة الدخول (تعيد الـ UserId في حال النجاح، أو رسالة خطأ)
    suspend fun authenticateUser(phone: String, name: String, isRegisterMode: Boolean): Result<String>
    fun saveSessionLocally(userId: String, name: String, phone: String)
    fun checkIsLoggedIn(): Boolean
}

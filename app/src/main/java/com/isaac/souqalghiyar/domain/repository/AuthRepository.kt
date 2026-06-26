package com.isaac.souqalghiyar.domain.repository

interface AuthRepository {
    suspend fun authenticateUser(phone: String, name: String, isRegisterMode: Boolean): Result<String>
    suspend fun checkUserExists(phone: String): Result<Boolean>
    fun saveSessionLocally(userId: String, name: String, phone: String)
    fun checkIsLoggedIn(): Boolean
    fun getUserId(): String?
}
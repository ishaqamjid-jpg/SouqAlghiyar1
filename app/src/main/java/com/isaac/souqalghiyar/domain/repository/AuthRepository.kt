package com.isaac.souqalghiyar.domain.repository

interface AuthRepository {
    // التحقق هل المستخدم موجود في قاعدة البيانات وجلب اسمه إذا كان موجوداً
    suspend fun checkUserExistsAndGetName(userId: String): Result<String?>
    
    // حفظ أو تحديث بيانات المستخدم في قاعدة البيانات
    suspend fun saveUserData(userId: String, phone: String, name: String): Result<Unit>
    
    // حفظ الجلسة محلياً
    fun saveSessionLocally(userId: String, name: String, phone: String)
    fun checkIsLoggedIn(): Boolean
    fun getUserId(): String?
}

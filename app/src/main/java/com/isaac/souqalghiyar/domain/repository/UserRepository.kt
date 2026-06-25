package com.isaac.souqalghiyar.domain.repository

import com.isaac.souqalghiyar.domain.model.users
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserData(userId: String): Flow<users?>
}

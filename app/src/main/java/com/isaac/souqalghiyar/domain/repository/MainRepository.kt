package com.isaac.souqalghiyar.domain.repository



import com.isaac.souqalghiyar.domain.model.Advertisement
import kotlinx.coroutines.flow.Flow

interface MainRepository {
    suspend fun getAdvertisements(): Flow<List<Advertisement>>
}
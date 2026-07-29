package com.isaac.souqalghiyar.di

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth // 👈 هذا الاستيراد مهم جداً
import com.google.firebase.firestore.FirebaseFirestore
import com.isaac.souqalghiyar.data.repository.AuthRepositoryImpl
import com.isaac.souqalghiyar.data.repository.MainRepositoryImpl
import com.isaac.souqalghiyar.data.repository.NotificationRepositoryImpl
import com.isaac.souqalghiyar.data.repository.OrderRepositoryImpl
import com.isaac.souqalghiyar.data.repository.UserRepositoryImpl
import com.isaac.souqalghiyar.domain.repository.AuthRepository
import com.isaac.souqalghiyar.domain.repository.MainRepository
import com.isaac.souqalghiyar.domain.repository.NotificationRepository
import com.isaac.souqalghiyar.domain.repository.OrderRepository
import com.isaac.souqalghiyar.domain.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    // 1. 👈 هذا هو الجزء الذي يحل المشكلة! يخبر Hilt بكيفية جلب FirebaseAuth
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        db: FirebaseFirestore,
        sharedPreferences: SharedPreferences
    ): AuthRepository {
        return AuthRepositoryImpl(db, sharedPreferences)
    }

    @Provides
    @Singleton
    fun provideMainRepository(firestore: FirebaseFirestore): MainRepository {
        return MainRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideOrderRepository(firestore: FirebaseFirestore): OrderRepository {
        return OrderRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideUserRepository(firestore: FirebaseFirestore): UserRepository {
        return UserRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(firestore: FirebaseFirestore): NotificationRepository {
        return NotificationRepositoryImpl(firestore)
    }
}
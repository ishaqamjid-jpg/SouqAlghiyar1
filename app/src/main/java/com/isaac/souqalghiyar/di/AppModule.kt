package com.isaac.souqalghiyar.di

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.isaac.souqalghiyar.data.repository.MainRepositoryImpl
import com.isaac.souqalghiyar.data.repository.OrderRepositoryImpl
import com.isaac.souqalghiyar.domain.repository.MainRepository
import com.isaac.souqalghiyar.domain.repository.OrderRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // توفير Firebase Firestore للاتصال بقاعدة البيانات
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    // توفير SharedPreferences (ملف الجلسة تذكرني)
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    }

    // توفير MainRepository الخاص بالشاشة الرئيسية وعرض الإعلانات
    @Provides
    @Singleton
    fun provideMainRepository(firestore: FirebaseFirestore): MainRepository {
        return MainRepositoryImpl(firestore)
    }

    // توفير OrderRepository الخاص برفع طلبات قطع الغيار الجديدة
    @Provides
    @Singleton
    fun provideOrderRepository(firestore: FirebaseFirestore): OrderRepository {
        return OrderRepositoryImpl(firestore)
    }

}

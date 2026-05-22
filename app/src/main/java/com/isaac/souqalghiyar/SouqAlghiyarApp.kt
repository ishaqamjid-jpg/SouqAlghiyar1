package com.isaac.souqalghiyar

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SouqAlghiyarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // يمكنك هنا تهيئة أي مكتبات أخرى تعمل عند بداية تشغيل التطبيق
    }
}

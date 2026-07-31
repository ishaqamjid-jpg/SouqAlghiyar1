package com.isaac.souqalghiyar.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.isaac.souqalghiyar.MainActivity
import com.isaac.souqalghiyar.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // حفظ التوكن محلياً لتحديثه في الواجهة أو عند تسجيل الدخول
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("fcm_token", token).apply()
        Log.d("FCM_CLIENT", "New Token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // استخراج البيانات من الإشعار سواء كان Notification Payload أو Data Payload
        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "سوق الغيار"
        val message = remoteMessage.data["message"] ?: remoteMessage.notification?.body ?: "لديك إشعار جديد"

        // استخراج رقم الطلب إذا كان مبعوثاً من السيرفر (Cloud Functions)
        val orderNumber = remoteMessage.data["order_number"] ?: ""

        showNotification(title, message, orderNumber)
    }

    private fun showNotification(title: String, message: String, orderNumber: String) {
        // تجهيز الـ Intent للانتقال إلى MainActivity عند الضغط على الإشعار
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            // تمرير رقم الطلب لكي نتمكن لاحقاً من فتح شاشة الطلبات وتحديد الطلب
            putExtra("order_number", orderNumber)
        }

        // استخدام UPDATE_CURRENT لضمان تحديث البيانات (رقم الطلب) في الـ Intent
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // جعل كل إشعار مستقلاً
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "client_notifications_channel"

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            // ⚠️ ملاحظة: يُفضل أن تكون الأيقونة بخلفية شفافة (PNG) باللون الأبيض
            .setSmallIcon(R.drawable.logo3)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // لضمان إصدار صوت واهتزاز

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // إنشاء قناة الإشعارات (إجباري لأندرويد 8.0 وما فوق)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "إشعارات فواتير العملاء",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة مخصصة لاستقبال تنبيهات الفواتير من سوق الغيار"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // عرض الإشعار برقم تعريفي فريد لكي لا يمسح الإشعار الذي قبله
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
package com.isaac.souqalghiyar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.isaac.souqalghiyar.presentation.login.LoginScreen
import com.isaac.souqalghiyar.presentation.main.MainScreen
import com.isaac.souqalghiyar.presentation.request_parts.RequestPartsScreen
import com.isaac.souqalghiyar.presentation.orders.OrdersScreen
import com.isaac.souqalghiyar.presentation.notifications.NotificationsScreen
import com.isaac.souqalghiyar.ui.theme.SouqAlghiyarTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean -> }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        askNotificationPermission()

        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)
        val savedUserId = sharedPref.getString("user_id", "") ?: ""

        // استخراج توكن الإشعارات وحفظه فور تشغيل التطبيق لضمان التحديث المستمر
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                sharedPref.edit().putString("fcm_token", token).apply()
                // إذا كان مسجلاً، نحدث التوكن في الفايربيز
                if (isLoggedIn && savedUserId.isNotEmpty()) {
                    FirebaseFirestore.getInstance().collection("users").document(savedUserId)
                        .update("fcm_token", token)
                }
            }
        }

        setContent {
            SouqAlghiyarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val startDestination = if (isLoggedIn && savedUserId.isNotEmpty()) "main/$savedUserId" else "login"

                    NavHost(navController = navController, startDestination = startDestination) {
                        
                        composable("login") {
                            LoginScreen(
                                navigateToMain = { userId ->
                                    // هنا يمكنك أيضاً التأكد من تحديث التوكن عند الدخول لأول مرة
                                    navController.navigate("main/$userId") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("main/{userId}") { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: ""
                            MainScreen(
                                userId = userId,
                                navigateToRequestParts = { brandName, vehicleName, vehicleModel, manufacture, vinNumber ->
                                    val safeVin = if (vinNumber.isBlank()) "غير_محدد" else vinNumber.replace("/", "-")
                                    val safeBrand = brandName.replace("/", "-")
                                    val safeName = vehicleName.replace("/", "-")
                                    val safeModel = vehicleModel.replace("/", "-")
                                    val safeManuf = manufacture.replace("/", "-")
                                    navController.navigate("request_parts/$userId/$safeBrand/$safeName/$safeModel/$safeManuf/$safeVin")
                                },
                                navigateToOrders = { passedUserId ->
                                    navController.navigate("orders/$passedUserId")
                                },
                                navigateToNotifications = { passedUserId ->
                                    navController.navigate("notifications/$passedUserId")
                                },
                                navigateToLogin = {
                                    sharedPref.edit().clear().apply()
                                    navController.navigate("login") {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        // (باقي الشاشات كما هي، بالإضافة إلى شاشة الإشعارات)
                        composable("notifications/{userId}") { backStackEntry ->
                            val routeUserId = backStackEntry.arguments?.getString("userId") ?: ""
                            val finalUserId = routeUserId.ifEmpty { sharedPref.getString("user_id", "") ?: "" }
                            NotificationsScreen(userId = finalUserId, onNavigateBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

package com.isaac.souqalghiyar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
    ) { isGranted: Boolean ->
        if (!isGranted) Log.e("FCM", "Notification Permission Denied")
    }

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

        // استخراج توكن الإشعارات وحفظه فور تشغيل التطبيق
        if (isLoggedIn && savedUserId.isNotEmpty()) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    sharedPref.edit().putString("fcm_token", token).apply()
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
                                    val token = sharedPref.getString("fcm_token", "") ?: ""
                                    if (token.isNotEmpty()) {
                                        FirebaseFirestore.getInstance().collection("users").document(userId)
                                            .update("fcm_token", token)
                                    }

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
                                    val safeVin = vinNumber.ifBlank { "غير_محدد" }.replace("/", "-")
                                    val safeBrand = brandName.ifBlank { "غير_محدد" }.replace("/", "-")
                                    val safeName = vehicleName.ifBlank { "غير_محدد" }.replace("/", "-")
                                    val safeModel = vehicleModel.ifBlank { "غير_محدد" }.replace("/", "-")
                                    val safeManuf = manufacture.ifBlank { "غير_محدد" }.replace("/", "-")
                                    val safeUserId = userId.ifBlank { "unknown_user" }

                                    navController.navigate("request_parts/$safeUserId/$safeBrand/$safeName/$safeModel/$safeManuf/$safeVin")
                                },
                                navigateToOrders = { passedUserId ->
                                    val safeId = passedUserId.ifBlank { "unknown_user" }
                                    navController.navigate("orders/$safeId")
                                },
                                navigateToNotifications = { passedUserId ->
                                    val safeId = passedUserId.ifBlank { "unknown_user" }
                                    navController.navigate("notifications/$safeId")
                                },
                                navigateToLogin = {
                                    if (userId.isNotEmpty()) {
                                        FirebaseFirestore.getInstance().collection("users").document(userId)
                                            .update("fcm_token", "")
                                    }

                                    sharedPref.edit().clear().apply()
                                    // مسح الـ Backstack بالكامل لضمان عدم العودة
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable("request_parts/{userId}/{brandName}/{vehicleName}/{vehicleModel}/{manufacture}/{vinNumber}") { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId")?.replace("unknown_user", "") ?: ""
                            val brandName = backStackEntry.arguments?.getString("brandName")?.replace("غير_محدد", "")?.replace("-", "/") ?: ""
                            val vehicleName = backStackEntry.arguments?.getString("vehicleName")?.replace("غير_محدد", "")?.replace("-", "/") ?: ""
                            val vehicleModel = backStackEntry.arguments?.getString("vehicleModel")?.replace("vehicleName", "")?.replace("-", "/") ?: ""
                            val manufacture = backStackEntry.arguments?.getString("manufacture")?.replace("غير_محدد", "")?.replace("-", "/") ?: ""
                            val vinNumber = backStackEntry.arguments?.getString("vinNumber")?.replace("غير_محدد", "")?.replace("-", "/") ?: ""

                            RequestPartsScreen(
                                userId = userId,
                                brandName = brandName,
                                vehicleName = vehicleName,
                                vehicleModel = vehicleModel,
                                manufacture = manufacture,
                                vinNumber = vinNumber,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("orders/{userId}") { backStackEntry ->
                            val routeUserId = backStackEntry.arguments?.getString("userId")?.replace("unknown_user", "") ?: ""
                            val finalUserId = routeUserId.ifEmpty { sharedPref.getString("user_id", "") ?: "" }

                            OrdersScreen(
                                userId = finalUserId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("notifications/{userId}") { backStackEntry ->
                            val routeUserId = backStackEntry.arguments?.getString("userId")?.replace("unknown_user", "") ?: ""
                            val finalUserId = routeUserId.ifEmpty { sharedPref.getString("user_id", "") ?: "" }

                            NotificationsScreen(
                                userId = finalUserId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

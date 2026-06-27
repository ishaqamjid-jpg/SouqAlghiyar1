package com.isaac.souqalghiyar

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.isaac.souqalghiyar.presentation.login.LoginScreen
import com.isaac.souqalghiyar.presentation.main.MainScreen
import com.isaac.souqalghiyar.presentation.request_parts.RequestPartsScreen
import com.isaac.souqalghiyar.presentation.orders.OrdersScreen
import com.isaac.souqalghiyar.ui.theme.SouqAlghiyarTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SouqAlghiyarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)
                    val savedUserId = sharedPref.getString("user_id", "") ?: ""

                    val startDestination = if (isLoggedIn && savedUserId.isNotEmpty()) "main/$savedUserId" else "login"

                    NavHost(navController = navController, startDestination = startDestination) {
                        
                        composable("login") {
                            LoginScreen(
                                navigateToMain = { userId ->
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
                                    // تمرير بيانات السيارة إلى شاشة تعبئة القطع
                                    val safeVin = if (vinNumber.isBlank()) "غير_محدد" else vinNumber.replace("/", "-")
                                    val safeBrand = brandName.replace("/", "-")
                                    val safeName = vehicleName.replace("/", "-")
                                    val safeModel = vehicleModel.replace("/", "-")
                                    val safeManuf = manufacture.replace("/", "-")

                                    navController.navigate("request_parts/$userId/$safeBrand/$safeName/$safeModel/$safeManuf/$safeVin")
                                },
                                navigateToOrders = { passedUserId ->
                                    // تمرير الـ userId الذي يصل من الواجهة الرئيسية إلى مسار شاشة الطلبات
                                    navController.navigate("orders/$passedUserId")
                                },
                                navigateToLogin = {
                                    // 1. مسح بيانات تسجيل الدخول من SharedPreferences
                                    sharedPref.edit().apply {
                                        putBoolean("is_logged_in", false)
                                        putString("user_id", "")
                                        apply()
                                    }
                                    
                                    // 2. التوجيه لشاشة تسجيل الدخول ومسح مكدس الشاشات (Back Stack) بالكامل
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true } // لمنع العودة للشاشة الرئيسية بزر الرجوع
                                    }
                                }
                            )
                        }

                        composable("request_parts/{userId}/{brandName}/{vehicleName}/{vehicleModel}/{manufacture}/{vinNumber}") { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: ""
                            val brandName = backStackEntry.arguments?.getString("brandName")?.replace("-", "/") ?: ""
                            val vehicleName = backStackEntry.arguments?.getString("vehicleName")?.replace("-", "/") ?: ""
                            val vehicleModel = backStackEntry.arguments?.getString("vehicleModel")?.replace("-", "/") ?: ""
                            val manufacture = backStackEntry.arguments?.getString("manufacture")?.replace("-", "/") ?: ""
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

                        // إضافة استقبال المتغير {userId} في مسار الطلبات لضمان عدم ضياع المعرف
                        composable("orders/{userId}") { backStackEntry ->
                            val routeUserId = backStackEntry.arguments?.getString("userId") ?: ""
                            // كخطوة أمان إضافية: لو لم يصل من الـ Navigation، نأخذه من الـ SharedPreferences
                            val finalUserId = routeUserId.ifEmpty { sharedPref.getString("user_id", "") ?: "" }
                            
                            OrdersScreen(
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

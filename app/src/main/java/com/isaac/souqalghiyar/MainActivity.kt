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
import com.isaac.souqalghiyar.presentation.orders.OrdersScreen
import com.isaac.souqalghiyar.presentation.request_parts.RequestPartsScreen
import com.isaac.souqalghiyar.ui.theme.SouqAlghiyarTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // فحص "تذكرني" (Remember Me) من الذاكرة المحلية
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)
        val savedUserId = sharedPref.getString("user_id", "") ?: ""

        // تحديد واجهة البداية بناءً على حالة تسجيل الدخول
        val startDest = if (isLoggedIn && savedUserId.isNotEmpty()) "main/$savedUserId" else "login"

        setContent {
            SouqAlghiyarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = startDest) {
                        
                        // 1. شاشة تسجيل الدخول
                        composable("login") {
                            LoginScreen(
                                navigateToMain = { userId ->
                                    // الانتقال للرئيسية وحذف شاشة الدخول من المكدس لمنع الرجوع إليها
                                    navController.navigate("main/$userId") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 2. الشاشة الرئيسية
                        composable("main/{userId}") { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: ""
                            MainScreen(
                                userId = userId,
                                navigateToRequestParts = { vName, vModel, vinPic -> 
                                    // الانتقال لشاشة تعبئة القطع وتمرير بيانات السيارة
                                    navController.navigate("request_parts/$userId/$vName/$vModel/$vinPic") 
                                },
                                navigateToOrders = { 
                                    // الانتقال لشاشة الطلبات المعلقة والسابقة
                                    navController.navigate("orders") 
                                }
                            )
                        }

                        // 3. شاشة طلب قطع الغيار
                        composable("request_parts/{userId}/{vName}/{vModel}/{vinPic}") { backStackEntry ->
                            RequestPartsScreen(
                                userId = backStackEntry.arguments?.getString("userId") ?: "",
                                vehicleName = backStackEntry.arguments?.getString("vName") ?: "",
                                vehicleModel = backStackEntry.arguments?.getString("vModel") ?: "",
                                picVinNumber = backStackEntry.arguments?.getString("vinPic") ?: "",
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 4. شاشة الطلبات (المعلقة والمنتهية)
                        composable("orders") { 
                            // نستخرج المعرف الذي تم حفظه عند الدخول لجلبه مباشرة
                            val currentUserId = sharedPref.getString("user_id", "") ?: ""
                            
                            OrdersScreen(
                                userId = currentUserId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

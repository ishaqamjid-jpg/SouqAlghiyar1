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

                        // 3. شاشة طلب قطع الغيار (تم حجز مسارها برمجياً لبرمجتها لاحقاً)
                        composable("request_parts/{userId}/{vName}/{vModel}/{vinPic}") { backStackEntry ->
                            // سيتم وضع واجهة RequestPartsScreen هنا لاحقاً
                        }

                        // 4. شاشة الطلبات (المعلقة والمنتهية)
                        composable("orders") { 
                            // سيتم وضع واجهة OrdersScreen هنا لاحقاً
                        }
                    }
                }
            }
        }
    }
}

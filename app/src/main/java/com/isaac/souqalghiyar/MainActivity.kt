package com.isaac.souqalghiyar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        enableEdgeToEdge()
        setContent {
            SouqAlghiyarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // إعداد مدير التنقل (Navigation)
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "login") {
                        
                        // 1. شاشة تسجيل الدخول
                        composable("login") {
                            LoginScreen(
                                navigateToMain = { userId ->
                                    // الانتقال للرئيسية وحذف شاشة الدخول من المكدس (BackStack)
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
                                navigateToRequestParts = { 
                                    // سينتقل لاحقاً لشاشة تعبئة القطع
                                    navController.navigate("request_parts") 
                                },
                                navigateToOrders = { 
                                    // سينتقل لشاشة الطلبات المعلقة والمنتهية
                                    navController.navigate("orders") 
                                }
                            )
                        }

                        // شاشات وهمية مؤقتة حتى نبرمجها لاحقاً
                        composable("request_parts") { /* سنكتب كودها لاحقاً */ }
                        composable("orders") { /* سنكتب كودها لاحقاً */ }
                    }
                }
            }
        }
    }
}

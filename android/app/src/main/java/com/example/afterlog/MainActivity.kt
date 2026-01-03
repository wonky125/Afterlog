package com.example.afterlog

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
import com.example.afterlog.feature.home.HomeScreen
import com.example.afterlog.ui.navigation.Screen
import com.example.afterlog.feature.report.ReportDetailScreen
import com.example.afterlog.ui.theme.AfterLogTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AfterLogTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = Screen.Home.route) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                onNavigateToReport = {
                                    navController.navigate(Screen.ReportDetail.route)
                                }
                            )
                        }
                        composable(Screen.ReportDetail.route) {
                            ReportDetailScreen()
                        }
                    }
                }
            }
        }
    }
}

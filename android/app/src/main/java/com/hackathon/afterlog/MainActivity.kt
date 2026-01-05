package com.hackathon.afterlog

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
import com.hackathon.afterlog.feature.home.HomeScreen
import com.hackathon.afterlog.ui.navigation.Screen
import com.hackathon.afterlog.feature.report.ReportDetailScreen
import com.hackathon.afterlog.ui.theme.AfterLogTheme
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
                                onNavigateToReport = { sessionId ->
                                    navController.navigate(Screen.ReportDetail.createRoute(sessionId))
                                }
                            )
                        }
                        composable(
                            route = Screen.ReportDetail.route
                        ) { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: "last_session"
                            ReportDetailScreen(sessionId = sessionId)
                        }
                    }
                }
            }
        }
    }
}

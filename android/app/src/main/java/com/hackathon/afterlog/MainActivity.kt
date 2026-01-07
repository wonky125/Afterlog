package com.hackathon.afterlog

import android.os.Bundle
import android.view.InputDevice
import android.view.MotionEvent
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
import com.hackathon.afterlog.feature.report.components.VideoPlayerScreen
import com.hackathon.afterlog.ui.theme.AfterLogTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        val isPointerHover = ev.actionMasked == MotionEvent.ACTION_HOVER_ENTER ||
            ev.actionMasked == MotionEvent.ACTION_HOVER_MOVE ||
            ev.actionMasked == MotionEvent.ACTION_HOVER_EXIT
        val isMouseOrStylus = (ev.source and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE ||
            (ev.source and InputDevice.SOURCE_STYLUS) == InputDevice.SOURCE_STYLUS

        if (isPointerHover && isMouseOrStylus) {
            // Workaround for Compose hover-exit crash on some Samsung devices.
            return true
        }

        return super.dispatchGenericMotionEvent(ev)
    }

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
                            ReportDetailScreen(
                                sessionId = sessionId,
                                onNavigateToVideo = { videoPath ->
                                    navController.navigate(Screen.VideoPlayer.createRoute(videoPath))
                                }
                            )
                        }
                        composable(Screen.VideoPlayer.route) { backStackEntry ->
                            val videoPath = backStackEntry.arguments?.getString("videoPath") ?: ""
                            // Path was encoded, so decode it
                            val decodedPath = java.net.URLDecoder.decode(videoPath, "UTF-8")
                            VideoPlayerScreen(
                                videoPath = decodedPath,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.hackathon.afterlog.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Guide : Screen("guide")
    object ReportDetail : Screen("report_detail/{sessionId}") {
        fun createRoute(sessionId: String) = "report_detail/$sessionId"
    }
    object VideoPlayer : Screen("video_player/{videoPath}") {
        fun createRoute(videoPath: String): String {
            // Encode path as it might contain slashes
            val encodedPath = java.net.URLEncoder.encode(videoPath, "UTF-8")
            return "video_player/$encodedPath"
        }
    }
}

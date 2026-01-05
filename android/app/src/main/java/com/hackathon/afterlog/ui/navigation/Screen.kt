package com.hackathon.afterlog.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object ReportDetail : Screen("report_detail/{sessionId}") {
        fun createRoute(sessionId: String) = "report_detail/$sessionId"
    }
}

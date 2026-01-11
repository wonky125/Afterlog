package com.hackathon.afterlog.feature.report.debug

import com.hackathon.afterlog.feature.report.data.MockReportData

object DebugConfig {
    /**
     * Set this to TRUE to force using mock data instead of real API calls.
     * Useful for UI testing and demonstrations.
     */
    const val USE_MOCK_DATA = false

    /**
     * Show debug-only actions in the report UI (e.g., re-analyze).
     */
    const val SHOW_DEBUG_ACTIONS = true

    /**
     * Select which mock data to use.
     * - MockReportData.realDataSimulation : The Arkham Manor case (from your JSON)
     * - MockReportData.terrorInArchives : A generic test case
     */
    val ACTIVE_MOCK_DATA = MockReportData.realDataSimulation
}

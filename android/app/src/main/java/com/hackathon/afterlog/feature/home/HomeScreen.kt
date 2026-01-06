package com.hackathon.afterlog.feature.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackathon.afterlog.service.AfterLogService
import com.hackathon.afterlog.ui.components.TerminalSurface
import com.hackathon.afterlog.ui.theme.SpaceTerminalColors
import com.hackathon.afterlog.ui.theme.SpaceTerminalTypography
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onNavigateToReport: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(checkPermissions(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
    }

    // Gemini Test State
    val testResult by viewModel.testResult.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()

    TerminalSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Section
            Text(
                text = "AEGIS-7 TERMINAL",
                style = SpaceTerminalTypography.logTitle.copy(fontSize = 28.sp),
                color = SpaceTerminalColors.PrimaryGreen
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SECURE_CONN_ESTABLISHED",
                style = SpaceTerminalTypography.systemStatus,
                color = SpaceTerminalColors.SecondaryCyan
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (!hasPermissions) {
                TerminalButton(
                    text = "GRANT_ACCESS_PERMISSIONS",
                    isWarning = true,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            )
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.RECORD_AUDIO
                                )
                            )
                        }
                    }
                )
            } else {
                // Service Control Section
                TerminalSectionHeader("BACKGROUND_PROCESS_DAEMON")
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                         TerminalButton(
                            text = "INIT_SERVICE",
                            onClick = {
                                val intent = Intent(context, AfterLogService::class.java)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                                Toast.makeText(context, "Protocol Initiated", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        TerminalButton(
                            text = "KILL_PROCESS",
                            isWarning = true,
                            onClick = {
                                val intent = Intent(context, AfterLogService::class.java)
                                context.stopService(intent)
                                Toast.makeText(context, "Protocol Terminated", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                TerminalButton(
                    text = "DEBUG: SIMULATE_BIO_THREAT (SCREAM)",
                    onClick = {
                       val intent = Intent(context, AfterLogService::class.java).apply {
                            action = AfterLogService.ACTION_SIMULATE_SCREAM
                        }
                        context.startService(intent)
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Gemini Analysis Section
                TerminalSectionHeader("MOTH_ER AI CORE")

                TerminalButton(
                    text = "ACCESS_BLACK_BOX_LOGS",
                    onClick = onNavigateToReport
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                TerminalButton(
                    text = if (isTesting) "PINGING_SATELLITE..." else "TEST_UPLINK_CONNECTION",
                    onClick = { viewModel.testGeminiConnection() },
                    isEnabled = !isTesting
                )

                // Test Result Display
                testResult?.let { result ->
                    if (result.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (result.contains("Success")) SpaceTerminalColors.PrimaryGreen else SpaceTerminalColors.WarningRed)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = ">> RESPONSE: $result",
                                style = SpaceTerminalTypography.systemStatus,
                                color = if (result.contains("Success")) SpaceTerminalColors.PrimaryGreen else SpaceTerminalColors.WarningRed
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "V1.2.0 // UNAUTHORIZED PERSONNEL WILL BE TERMINATED",
                style = SpaceTerminalTypography.timestamp,
                color = SpaceTerminalColors.TextDim,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun TerminalSectionHeader(title: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = ":: $title ::",
            style = SpaceTerminalTypography.systemStatus,
            color = SpaceTerminalColors.TextDim
        )
        HorizontalDivider(color = SpaceTerminalColors.TextDim.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun TerminalButton(
    text: String,
    onClick: () -> Unit,
    isWarning: Boolean = false,
    isEnabled: Boolean = true
) {
    val mainColor = if (isWarning) SpaceTerminalColors.WarningRed else SpaceTerminalColors.PrimaryGreen
    
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = mainColor,
            disabledContentColor = SpaceTerminalColors.TextDim
        ),
        border = BorderStroke(1.dp, if (isEnabled) mainColor else SpaceTerminalColors.TextDim),
        shape = androidx.compose.ui.graphics.RectangleShape // Retro sharp corners
    ) {
        Text(
             text = text,
             style = SpaceTerminalTypography.systemStatus
        )
    }
}

private fun checkPermissions(context: Context): Boolean {
    val camera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
    val audio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
    
    val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        PackageManager.PERMISSION_GRANTED
    }

    return camera == PackageManager.PERMISSION_GRANTED && 
           audio == PackageManager.PERMISSION_GRANTED &&
           notifications == PackageManager.PERMISSION_GRANTED
}

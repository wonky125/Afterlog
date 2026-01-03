package com.hackathon.afterlog.feature.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.hackathon.afterlog.feature.result.GameResultViewModel
import com.hackathon.afterlog.feature.result.ResultUiState
import java.io.File

@Composable
fun ReportDetailScreen(
    viewModel: GameResultViewModel = hiltViewModel(),
    sessionId: String = "last_session" // In real app, pass this via NavArgs
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.loadSessionData(sessionId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)) // Dark Noir Background
    ) {
        when (val state = uiState) {
            is ResultUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
                Text(
                    text = "Investigating Evidence...",
                    color = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 64.dp)
                )
            }
            is ResultUiState.Analyzing -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI Detective is analyzing logic...", color = Color.White)
                    Text("Found ${state.logs.size} clues.", color = Color.Gray)
                }
            }
            is ResultUiState.Error -> {
                Text(
                    text = "Case Closed (Error): ${state.message}",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is ResultUiState.Success -> {
                ReportContent(state.report, state.logs)
            }
        }
    }
}

@Composable
fun ReportContent(reportText: String, logs: List<com.hackathon.afterlog.data.local.entities.MediaLogEntity>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        item {
            Text(
                text = "THE AFTERLOG CHRONICLE",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalDivider(color = Color.LightGray, thickness = 2.dp)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // AI Generated Story
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = reportText,
                        color = Color.White,
                        fontFamily = FontFamily.Serif,
                        lineHeight = 24.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Evidence Gallery (Video Thumbnails / Logs)
        item {
            Text(
                text = "EVIDENCE LOG",
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(logs) { log ->
            EvidenceItem(log)
            HorizontalDivider(color = Color.DarkGray)
        }
    }
}

@Composable
fun EvidenceItem(log: com.hackathon.afterlog.data.local.entities.MediaLogEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail (Using Coil)
        // Since we have video MPs, Coil can fetch the first frame or use a placeholder
        // For 'SCHREAM_MARKER', we might not have a file visually or it might be the LOG file.
        // If it's a video chunk, show it.
        
        if (log.filePath.endsWith(".mp4")) {
            AsyncImage(
                model = File(log.filePath),
                contentDescription = "Evidence Video",
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.Black),
                contentScale = ContentScale.Crop
            )
        } else {
             // Audio Icon or Placeholder
             Box(
                 modifier = Modifier
                     .size(80.dp)
                     .background(Color.DarkGray),
                 contentAlignment = Alignment.Center
             ) {
                 Text("AUDIO", color = Color.White, fontSize = 10.sp)
             }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = log.type.toString(),
                color = Color.Red, // Highlight crucial events
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = "Time: ${java.text.SimpleDateFormat("HH:mm:ss").format(log.timestamp)}",
                color = Color.LightGray,
                fontSize = 12.sp
            )
            if (log.decibel != null) {
                Text(
                    text = "Volume: ${log.decibel} dB",
                    color = Color.Yellow,
                    fontSize = 12.sp
                )
            }
        }
    }
}

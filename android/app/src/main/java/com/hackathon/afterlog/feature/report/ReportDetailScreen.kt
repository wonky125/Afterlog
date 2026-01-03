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
    sessionId: String = "last_session" 
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
                LoadingView()
            }
            is ResultUiState.Analyzing -> {
                AnalyzingView(count = state.logs.size)
            }
            is ResultUiState.Error -> {
                ErrorView(state.message)
            }
            is ResultUiState.Success -> {
                if (state.report != null) {
                    NewspaperView(state.report, state.logs)
                } else {
                    RawTextFallbackView(state.rawText ?: "No evidence found.", state.logs)
                }
            }
        }
    }
}

@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFFD4AF37)) // Gold
        Text(
            text = "Opening Case File...",
            color = Color.Gray,
            modifier = Modifier.padding(top = 64.dp)
        )
    }
}

@Composable
fun AnalyzingView(count: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Color.Red)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Connecting clues...", color = Color.White)
        Text("Analyzing $count pieces of evidence.", color = Color.Gray)
    }
}

@Composable
fun ErrorView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Case Closed (Error): $message", color = Color.Red)
    }
}

@Composable
fun NewspaperView(report: com.hackathon.afterlog.data.model.GeminiReport, logs: List<com.hackathon.afterlog.data.local.entities.MediaLogEntity>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFFF5E6CA)) // Old Paper Color
    ) {
        // Newspaper Header
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "THE AFTERLOG",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 40.sp,
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                HorizontalDivider(color = Color.Black, thickness = 3.dp)
                Text(
                    text = "VOL. I  •  DETECTIVE'S FINAL REPORT",
                    fontFamily = FontFamily.Serif,
                    fontSize = 12.sp,
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                HorizontalDivider(color = Color.Black, thickness = 1.dp)
            }
        }

        // Headline
        item {
            Text(
                text = report.headline.uppercase(),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 36.sp,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Summary (Lead)
        item {
            Text(
                text = report.summary,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = Color(0xFF2C2C2C),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Article Body
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                HorizontalDivider(color = Color.Black, thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))
                
                Text(
                    text = report.article,
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth()
                )
                
                HorizontalDivider(color = Color.Black, thickness = 1.dp, modifier = Modifier.padding(top = 12.dp))
            }
        }

        // Atmosphere & Verdict Columns
        item {
            Row(modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionHeader("ATMOSPHERE")
                    Text(
                        text = report.atmosphere,
                        fontFamily = FontFamily.Serif,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SectionHeader("VERDICT")
                    Text(
                        text = report.verdict,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF8B0000) // Blood Red Verdict
                    )
                }
            }
            HorizontalDivider(color = Color.Black, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
        }

        // Timeline
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader("TIMELINE OF EVENTS", modifier = Modifier.padding(horizontal = 16.dp))
        }

        items(report.timeline) { event ->
            TimelineEventCard(event)
        }
        
        item {
             Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        color = Color.DarkGray,
        modifier = modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun TimelineEventCard(event: com.hackathon.afterlog.data.model.TimelineEvent) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = event.timestamp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.speaker,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF553311) // Leather Brown
                )
            }
            Text(
                text = event.event,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )
            Text(
                text = event.description,
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                color = Color.DarkGray
            )
            if (event.decibel != null) {
                 Text(
                    text = "Analyzed Volume: ${event.decibel} dB",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
            HorizontalDivider(color = Color.LightGray, modifier = Modifier.padding(top = 8.dp))
        }
    }
}


@Composable
fun RawTextFallbackView(rawText: String, logs: List<com.hackathon.afterlog.data.local.entities.MediaLogEntity>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Unformatted Report",
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = rawText,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
        }
        items(logs) { log ->
            Text(log.toString(), color = Color.Gray)
        }
    }
}

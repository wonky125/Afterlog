package com.hackathon.afterlog.feature.report

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hackathon.afterlog.data.local.entities.MediaLogEntity
import com.hackathon.afterlog.data.model.GeminiReport
import com.hackathon.afterlog.data.model.TimelineEvent
import com.hackathon.afterlog.feature.result.GameResultViewModel
import com.hackathon.afterlog.feature.result.ResultUiState
import com.hackathon.afterlog.ui.components.*
import com.hackathon.afterlog.ui.theme.LoraFamily
import com.hackathon.afterlog.ui.theme.NewspaperColors
import com.hackathon.afterlog.ui.theme.NewspaperTypography
import com.hackathon.afterlog.ui.theme.SpecialEliteFamily
import kotlinx.coroutines.delay

private const val TAG = "ReportDetailScreen"

@Composable
fun ReportDetailScreen(
    viewModel: GameResultViewModel = hiltViewModel(),
    sessionId: String = "last_session"
) {
    Log.d(TAG, "ReportDetailScreen composable launched with sessionId: $sessionId")
    val uiState by viewModel.uiState.collectAsState()
    var showLoadingScreen by remember { mutableStateOf(true) }

    LaunchedEffect(uiState) {
        Log.d(TAG, "UI State changed: ${uiState::class.simpleName}")
    }

    // This effect ensures a minimum loading time for cinematic effect
    // and triggers data loading.
    LaunchedEffect(sessionId) {
        Log.d(TAG, "LaunchedEffect for data loading triggered.")
        val minLoadingTime = 3000L
        val startTime = System.currentTimeMillis()

        // Start data loading immediately
        Log.d(TAG, "Calling viewModel.loadSessionData...")
        viewModel.loadSessionData(sessionId)
        Log.d(TAG, "viewModel.loadSessionData call finished.")

        val elapsedTime = System.currentTimeMillis() - startTime
        if (elapsedTime < minLoadingTime) {
            delay(minLoadingTime - elapsedTime)
        }

        Log.d(TAG, "Minimum loading time passed. Setting showLoadingScreen to false.")
        showLoadingScreen = false
    }

    TexturedBackground(
        modifier = Modifier.fillMaxSize(),
        baseColor = NewspaperColors.FreshPaper
    ) {
        if (showLoadingScreen || uiState is ResultUiState.Loading || uiState is ResultUiState.Analyzing) {
            Log.d(TAG, "Displaying CinematicLoadingView. showLoadingScreen=$showLoadingScreen, uiState=${uiState::class.simpleName}")
            val logCount = if (uiState is ResultUiState.Analyzing) (uiState as ResultUiState.Analyzing).logs.size else 0
            CinematicLoadingView(count = logCount)
        } else {
            Log.d(TAG, "Loading finished. Displaying result for state: ${uiState::class.simpleName}")
            when (val state = uiState) {
                is ResultUiState.Error -> {
                    Log.e(TAG, "ResultUiState.Error: ${state.message}")
                    ErrorView(state.message)
                }
                is ResultUiState.Success -> {
                    Log.d(TAG, "ResultUiState.Success. Report is ${if (state.report != null) "present" else "null"}.")
                    if (state.report != null) {
                        val isPlaying by viewModel.isPlaying.collectAsState()
                        val isTtsLoading by viewModel.isTtsLoading.collectAsState()

                        CinematicNewspaperView(
                            report = state.report,
                            isPlaying = isPlaying,
                            isTtsLoading = isTtsLoading,
                            onPlayClick = {
                                val textToRead = """
                                    ${state.report.headline}. 
                                    ${state.report.summary}. 
                                    ${state.report.article}.
                                    Verdict: ${state.report.verdict}
                                """.trimIndent()
                                Log.d(TAG, "Play button clicked. Text to read length: ${textToRead.length}")
                                viewModel.toggleNarration(textToRead)
                            }
                        )
                    } else {
                        Log.w(TAG, "Success state but report is null. Showing fallback view.")
                        RawTextFallbackView(state.rawText ?: "No evidence found.", state.logs)
                    }
                }
                is ResultUiState.Loading, is ResultUiState.Analyzing -> {
                    Log.d(TAG, "This should not happen. State ${uiState::class.simpleName} should be handled by loading view.")
                    /* Do nothing */
                }
            }
        }
    }
}

@Composable
fun CinematicLoadingView(count: Int) {
    val startMessage = if (count > 0) "Reviewing $count pieces of evidence..." else "Scanning archives..."
    var loadingText by remember { mutableStateOf(startMessage) }

    LaunchedEffect(Unit) {
        val messages = listOf(
            startMessage,
            "Connecting the dots...",
            "Drafting the headline...",
            "Setting the type...",
            "Inking the printing rollers...",
            "Printing the Extra edition..."
        )

        var index = 0
        while (true) {
            delay(1500)
            index = (index + 1) % messages.size
            loadingText = messages[index]
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NewspaperColors.HeadlineRed)
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = loadingText,
                style = NewspaperTypography.subheadline,
                textAlign = TextAlign.Center,
                color = NewspaperColors.InkBlack,
                modifier = Modifier.animateContentSize()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Do not close the case file.",
                style = NewspaperTypography.caption,
                color = NewspaperColors.InkGray
            )
        }
    }
}

@Composable
fun ErrorView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Case Closed (Error): $message",
            style = NewspaperTypography.body,
            color = NewspaperColors.ErrorRed
        )
    }
}

@Composable
fun CinematicNewspaperView(
    report: GeminiReport,
    isPlaying: Boolean = false,
    isTtsLoading: Boolean = false,
    onPlayClick: () -> Unit = {}
) {
    Log.d(TAG, "CinematicNewspaperView composed. Headline: ${report.headline}")
    var hasLanded by remember { mutableStateOf(false) }
    var headlineComplete by remember { mutableStateOf(false) }

    NewspaperEntranceAnimation(
        modifier = Modifier.fillMaxSize(),
        onLanded = {
            Log.d(TAG, "NewspaperEntranceAnimation: onLanded callback received.")
            hasLanded = true
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            item {
                Log.d(TAG, "LazyColumn: Composing NewspaperHeader")
                NewspaperHeader(
                    onPlayClick = onPlayClick,
                    isPlaying = isPlaying,
                    isLoading = isTtsLoading
                )
            }

            item {
                Log.d(TAG, "LazyColumn: Composing Headline. hasLanded: $hasLanded")
                if (hasLanded) {
                    TypewriterText(
                        text = report.headline.uppercase(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = NewspaperTypography.headline,
                        color = NewspaperColors.HeadlineBlack,
                        charDelayMs = 25L,
                        onComplete = {
                            Log.d(TAG, "TypewriterText: onComplete callback received.")
                            headlineComplete = true
                        }
                    )
                } else {
                    Spacer(modifier = Modifier.height(56.dp))
                }
            }

            if (report.imagePath != null) {
                Log.d(TAG, "LazyColumn: imagePath is present: ${report.imagePath}")
                item {
                    FadeInContent(
                        visible = headlineComplete,
                        delayMs = 50
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            val vintageMatrix = remember {
                                ColorMatrix(
                                    floatArrayOf(
                                        0.393f, 0.769f, 0.189f, 0f, 0f,
                                        0.349f, 0.686f, 0.168f, 0f, 0f,
                                        0.272f, 0.534f, 0.131f, 0f, 0f,
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(NewspaperColors.InkGray.copy(alpha = 0.1f))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(report.imagePath)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Evidence Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    colorFilter = ColorFilter.colorMatrix(vintageMatrix)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Fig 1. Visual evidence from the scene.",
                                style = NewspaperTypography.caption,
                                color = NewspaperColors.InkGray,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                Log.d(TAG, "LazyColumn: imagePath is null.")
            }

            item {
                Log.d(TAG, "LazyColumn: Composing Summary. headlineComplete: $headlineComplete")
                FadeInContent(
                    visible = headlineComplete,
                    delayMs = 100
                ) {
                    Text(
                        text = report.summary,
                        style = NewspaperTypography.subheadline,
                        color = NewspaperColors.InkGray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            item {
                Log.d(TAG, "LazyColumn: Composing Article. headlineComplete: $headlineComplete")
                FadeInContent(
                    visible = headlineComplete,
                    delayMs = 300
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        HorizontalDivider(
                            color = NewspaperColors.RuleLine,
                            thickness = 1.dp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            text = report.article,
                            style = NewspaperTypography.body,
                            color = NewspaperColors.InkBlack,
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(
                            color = NewspaperColors.RuleLine,
                            thickness = 1.dp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }

            item {
                FadeInContent(
                    visible = headlineComplete,
                    delayMs = 500
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionHeader("ATMOSPHERE")
                            Text(
                                text = report.atmosphere,
                                style = NewspaperTypography.body,
                                color = NewspaperColors.InkBlack
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SectionHeader("VERDICT")
                            Text(
                                text = report.verdict,
                                style = NewspaperTypography.body.copy(
                                    fontFamily = LoraFamily,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                ),
                                color = NewspaperColors.HeadlineRed
                            )
                        }
                    }
                    HorizontalDivider(
                        color = NewspaperColors.RuleLine,
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            item {
                FadeInContent(
                    visible = headlineComplete,
                    delayMs = 700
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(
                        text = "TIMELINE OF EVENTS",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            items(report.timeline) { event ->
                Log.d(TAG, "LazyColumn: Composing TimelineEventCard for event at ${event.timestamp}")
                FadeInContent(
                    visible = headlineComplete,
                    delayMs = 800
                ) {
                    TimelineEventCard(event)
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = NewspaperTypography.sectionHeader,
        color = NewspaperColors.InkGray,
        modifier = modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun TimelineEventCard(event: TimelineEvent) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NewspaperColors.AgedPaper.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            if (event.imagePath != null) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .padding(end = 12.dp)
                        .background(NewspaperColors.InkGray.copy(alpha = 0.1f))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(event.imagePath)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Event Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(ColorMatrix(
                            floatArrayOf(
                                0.393f, 0.769f, 0.189f, 0f, 0f,
                                0.349f, 0.686f, 0.168f, 0f, 0f,
                                0.272f, 0.534f, 0.131f, 0f, 0f,
                                0f, 0f, 0f, 1f, 0f
                            )
                        ))
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.timestamp,
                        style = NewspaperTypography.timestamp,
                        color = NewspaperColors.InkGray
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = event.speaker,
                        style = NewspaperTypography.timestamp.copy(
                            fontFamily = SpecialEliteFamily
                        ),
                        color = NewspaperColors.SpeakerA
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.event,
                    style = NewspaperTypography.body.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    ),
                    color = NewspaperColors.InkBlack
                )
                Text(
                    text = event.description,
                    style = NewspaperTypography.body,
                    color = NewspaperColors.InkGray
                )
                if (event.decibel != null) {
                    Text(
                        text = "Analyzed Volume: ${event.decibel} dB",
                        style = NewspaperTypography.caption,
                        color = NewspaperColors.LightRule
                    )
                }
            }
        }
    }
}

@Composable
fun RawTextFallbackView(rawText: String, logs: List<MediaLogEntity>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Unformatted Report",
                style = NewspaperTypography.sectionHeader,
                color = NewspaperColors.ErrorRed
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = rawText,
                style = NewspaperTypography.timestamp,
                color = NewspaperColors.InkBlack
            )
        }
        items(logs) { log ->
            Text(
                text = log.toString(),
                style = NewspaperTypography.caption,
                color = NewspaperColors.InkGray
            )
        }
    }
}

// Preview with mock data
@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun PreviewCinematicNewspaperView() {
    val mockReport = GeminiReport(
        headline = "Terror in the Archives: A Night of Unspeakable Horror",
        summary = "Our brave investigators ventured into the haunted library, only to discover horrors beyond imagination lurking in the shadows.",
        imagePath = "android.resource://com.hackathon.afterlog/drawable/sample_evidence",
        article = "The evening began innocuously enough, with our party of four entering the estate\'s grand library. Little did they know that ancient evil awaited...",
        atmosphere = "Tense, claustrophobic, with moments of sheer panic.",
        verdict = "INVESTIGATION FAILED - Madness consumed all.",
        timeline = listOf(
            TimelineEvent(
                timestamp = "00:15:32",
                speaker = "Speaker A",
                event = "First Encounter",
                description = "A strange noise echoed from the basement.",
                decibel = 85,
                imagePath = "android.resource://com.hackathon.afterlog/drawable/sample_evidence"
            ),
            TimelineEvent(
                timestamp = "00:42:17",
                speaker = "Speaker B",
                event = "The Revelation",
                description = "Ancient texts revealed the entity\'s true name.",
                decibel = null
            )
        )
    )

    TexturedBackground(modifier = Modifier.fillMaxSize()) {
        CinematicNewspaperView(
            report = mockReport,
            isPlaying = false,
            isTtsLoading = false,
            onPlayClick = {}
        )
    }
}

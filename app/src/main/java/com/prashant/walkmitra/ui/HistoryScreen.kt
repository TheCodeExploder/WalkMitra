@file:OptIn(ExperimentalMaterial3Api::class)

package com.prashant.walkmitra.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.prashant.walkmitra.data.WalkHistory
import com.prashant.walkmitra.data.loadWalkHistories
import java.io.File
import java.util.*

private fun formatDuration(ms: Long): String {
    val h = ms / 3600000
    val m = (ms / 60000) % 60
    val s = (ms / 1000) % 60
    return "%02d:%02d:%02d".format(h, m, s)
}


@Composable
fun HistoryScreen(navController: NavController, context: Context) {
    var sessions by remember { mutableStateOf<List<WalkHistory>>(emptyList()) }
    LaunchedEffect(Unit) {
        sessions = loadWalkHistories(context).sortedByDescending { it.date }
    }

    val recentSession = sessions.firstOrNull()
    val weeklySessions = sessions.take(7)
    val monthlySessions = sessions.take(30)
    val yearlySessions = sessions

    fun List<WalkHistory>.averageStats(): Pair<Int, Long> {
        val totalDistance = sumOf { it.distance.toInt() }
        val totalDuration = sumOf { it.duration }
        val count = size.coerceAtLeast(1)
        return totalDistance / count to totalDuration / count
    }

    val (weeklyDist, weeklyDur) = weeklySessions.averageStats()
    val (monthlyDist, monthlyDur) = monthlySessions.averageStats()
    val (yearlyDist, yearlyDur) = yearlySessions.averageStats()

    val summaryStats = listOf(
        "🗓 Weekly Avg\n \n⏱️ ${weeklyDur / 60000}m ${(weeklyDur / 1000) % 60}s \n 📏 $weeklyDist m",
        "🗓 Monthly Avg\n \n⏱️ ${monthlyDur / 60000}m ${(monthlyDur / 1000) % 60}s \n 📏 $monthlyDist m",
        "🗓 Yearly Avg\n \n⏱️ ${yearlyDur / 60000}m ${(yearlyDur / 1000) % 60}s \n 📏 $yearlyDist m"
    )

    val bitmap = recentSession?.screenshotPath?.let { path ->
        val file = File(path)
        if (file.exists()) android.graphics.BitmapFactory.decodeFile(file.absolutePath) else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Walk History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFB3E5FC), Color(0xFFFFE0B2)) // Dark stylish gradient
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        )
        {
            if (bitmap != null) {
                item {
                    Text(
                        text = "🗺️ Last Walk Map",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    GlassyCard {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Map Screenshot",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.77f)
                        )
                    }
                }
            }

            if (recentSession != null) {
                item {
                    Text(
                        text = "🔥 Last Walk Stats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    GlassyCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("📅 ${recentSession.date}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("⏱️ Duration: ${formatDuration(recentSession.duration)}")
                            Text("📏 Distance: ${recentSession.distance} m")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "📊 Summary Stats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(summaryStats.size) { index ->
                GlassyCard {
                    Text(
                        text = summaryStats[index],
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun GlassyCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.1f)
                    )
                )
            )
            .alpha(0.85f),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        content()
    }
}

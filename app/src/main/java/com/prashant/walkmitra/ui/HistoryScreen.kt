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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.json.JSONObject
import java.util.*

data class WalkSession(
    val date: String,
    val startTime: String,
    val endTime: String,
    val duration: String,
    val distance: Int,
    val calories: Int
)

@Composable
fun HistoryScreen(navController: NavController, context: Context) {
    val sharedPreferences = context.getSharedPreferences("walkmitra_history", Context.MODE_PRIVATE)
    val sessionSet = sharedPreferences.getStringSet("sessions", emptySet()) ?: emptySet()
    val sessions = sessionSet.mapNotNull { json ->
        try {
            val obj = JSONObject(json)
            WalkSession(
                date = obj.getString("date"),
                startTime = obj.getString("startTime"),
                endTime = obj.getString("endTime"),
                duration = obj.getString("duration"),
                distance = obj.getInt("distance"),
                calories = obj.getInt("calories")
            )
        } catch (e: Exception) {
            null
        }
    }.sortedByDescending { it.date + it.startTime }

    val recentSession = sessions.firstOrNull()
    val weeklySessions = sessions.take(7)
    val monthlySessions = sessions.take(30)
    val yearlySessions = sessions

    fun List<WalkSession>.averageStats(): Triple<Int, Int, Int> {
        val totalDistance = this.sumOf { it.distance }
        val totalCalories = this.sumOf { it.calories }
        val totalDuration = this.sumOf {
            val (h, m, s) = it.duration.split(":").mapNotNull { part -> part.toIntOrNull() }
                .let { t -> Triple(t.getOrNull(0) ?: 0, t.getOrNull(1) ?: 0, t.getOrNull(2) ?: 0) }
            h * 3600 + m * 60 + s
        }
        val count = this.size.coerceAtLeast(1)
        return Triple(totalDistance / count, totalCalories / count, totalDuration / count)
    }

    val (weeklyDist, weeklyCal, weeklyDur) = weeklySessions.averageStats()
    val (monthlyDist, monthlyCal, monthlyDur) = monthlySessions.averageStats()
    val (yearlyDist, yearlyCal, yearlyDur) = yearlySessions.averageStats()

    val summaryStats = listOf(
        "🗓 Weekly Avg\n \n⏱️ ${weeklyDur / 60}m ${weeklyDur % 60}s  \n 📏 $weeklyDist m  \n 🔥 $weeklyCal kcal",
        "🗓 Monthly Avg\n \n⏱️ ${monthlyDur / 60}m ${monthlyDur % 60}s \n 📏 $monthlyDist m \n 🔥 $monthlyCal kcal",
        "🗓 Yearly Avg\n \n ⏱️ ${yearlyDur / 60}m ${yearlyDur % 60}s \n 📏 $yearlyDist m  \n 🔥 $yearlyCal kcal"
    )

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
                        colors = listOf(Color(0x99FFFFFF), Color(0xCC292D3E)) // Dark stylish gradient
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        )
        {
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
                            Text("⏱️ Duration: ${recentSession.duration}")
                            Text("📏 Distance: ${recentSession.distance} m")
                            Text("🔥 Calories: ${recentSession.calories} kcal")
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

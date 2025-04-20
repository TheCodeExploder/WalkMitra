@file:OptIn(ExperimentalMaterial3Api::class)

package com.prashant.walkmitra.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.json.JSONObject
import java.util.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha

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
        "🗓 Weekly Avg\n⏱️ ${weeklyDur / 60}m ${weeklyDur % 60}s | 📏 $weeklyDist m | 🔥 $weeklyCal kcal",
        "🗓 Monthly Avg\n⏱️ ${monthlyDur / 60}m ${monthlyDur % 60}s | 📏 $monthlyDist m | 🔥 $monthlyCal kcal",
        "🗓 Yearly Avg\n⏱️ ${yearlyDur / 60}m ${yearlyDur % 60}s | 📏 $yearlyDist m | 🔥 $yearlyCal kcal"
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (recentSession != null) {
                item {
                    Text(
                        text = "🔥 Last Walk Stats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
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

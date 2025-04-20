@file:OptIn(ExperimentalMaterial3Api::class)

package com.prashant.walkmitra.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.prashant.walkmitra.data.loadWalkHistories
import com.prashant.walkmitra.data.WalkHistory
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.nativeCanvas

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

    val recentSessions = sessions.take(5)
    val weeklySessions = sessions.drop(5).take(7)
    val monthlySessions = sessions.drop(5).take(30)
    val yearlySessions = sessions.drop(5)

    fun List<WalkSession>.averageStats(): Triple<Int, Int, Int> {
        val totalDistance = this.sumOf { it.distance }
        val totalCalories = this.sumOf { it.calories }
        val totalDuration = this.sumOf {
            it.duration.split(":").let {
                val h = it.getOrNull(0)?.toIntOrNull() ?: 0
                val m = it.getOrNull(1)?.toIntOrNull() ?: 0
                val s = it.getOrNull(2)?.toIntOrNull() ?: 0
                (h * 3600 + m * 60 + s)
            }
        }
        val count = this.size.coerceAtLeast(1)
        return Triple(totalDistance / count, totalCalories / count, totalDuration / count)
    }

    val (weeklyDist, weeklyCal, weeklyDur) = weeklySessions.averageStats()
    val (monthlyDist, monthlyCal, monthlyDur) = monthlySessions.averageStats()
    val (yearlyDist, yearlyCal, yearlyDur) = yearlySessions.averageStats()

    val summaryStats = listOf(
        "Weekly Avg: $weeklyDist m, ${weeklyDur}s, $weeklyCal kcal",
        "Monthly Avg: $monthlyDist m, ${monthlyDur}s, $monthlyCal kcal",
        "Yearly Avg: $yearlyDist m, ${yearlyDur}s, $yearlyCal kcal"
    )

    val walkHistories = remember { mutableStateListOf<WalkHistory>() }
    LaunchedEffect(Unit) {
        walkHistories.clear()
        walkHistories.addAll(loadWalkHistories(context))
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
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (sessions.isEmpty() && walkHistories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No walk data yet.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "📈 Last 5 Walks Comparison Chart",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            LegendItem(Color(0xFF64B5F6), "Distance")
                            LegendItem(Color(0xFF81C784), "Duration")
                            LegendItem(Color(0xFFE57373), "Calories")
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth().height(240.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFAA9F2A2))
                        ) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                if (recentSessions.isEmpty()) return@Canvas

                                val maxDistance = recentSessions.maxOfOrNull { it.distance } ?: 1
                                val maxCalories = recentSessions.maxOfOrNull { it.calories } ?: 1
                                val maxDuration = recentSessions.maxOfOrNull {
                                    it.duration.split(":").let { parts ->
                                        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
                                        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                        val s = parts.getOrNull(2)?.toIntOrNull() ?: 0
                                        h * 3600 + m * 60 + s
                                    }
                                } ?: 1

                                val barWidth = size.width / (recentSessions.size * 4f)

                                recentSessions.forEachIndexed { i, session ->
                                    val xOffset = i * 4 * barWidth

                                    val durationSec = session.duration.split(":").let { parts ->
                                        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
                                        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                        val s = parts.getOrNull(2)?.toIntOrNull() ?: 0
                                        h * 3600 + m * 60 + s
                                    }

                                    val distHeight = (session.distance / maxDistance.toFloat()) * size.height
                                    val calHeight = (session.calories / maxCalories.toFloat()) * size.height
                                    val durHeight = (durationSec / maxDuration.toFloat()) * size.height

                                    drawRect(Color(0xFF64B5F6), topLeft = androidx.compose.ui.geometry.Offset(xOffset, size.height - distHeight), size = androidx.compose.ui.geometry.Size(barWidth, distHeight))
                                    drawRect(Color(0xFF81C784), topLeft = androidx.compose.ui.geometry.Offset(xOffset + barWidth, size.height - durHeight), size = androidx.compose.ui.geometry.Size(barWidth, durHeight))
                                    drawRect(Color(0xFFE57373), topLeft = androidx.compose.ui.geometry.Offset(xOffset + 2 * barWidth, size.height - calHeight), size = androidx.compose.ui.geometry.Size(barWidth, calHeight))

                                    drawIntoCanvas {
                                        val paint = Paint().apply {
                                            isAntiAlias = true
                                            color = android.graphics.Color.BLACK
                                            textSize = 28f
                                        }
                                        it.nativeCanvas.drawText("${session.distance}m", xOffset + barWidth / 4, size.height - distHeight - 8, paint)
                                        it.nativeCanvas.drawText("${durationSec}s", xOffset + barWidth + barWidth / 4, size.height - durHeight - 8, paint)
                                        it.nativeCanvas.drawText("${session.calories}kcal", xOffset + 2 * barWidth + barWidth / 4, size.height - calHeight - 8, paint)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "📊 Summary Stats",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(summaryStats.size) { index ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF2F7)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(summaryStats[index], style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun WalkCard(session: WalkSession) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📅 Date: ${session.date}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray)
                Spacer(Modifier.width(4.dp))
                Text("Start: ${formatTime(session.startTime)} | End: ${formatTime(session.endTime)}")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timelapse, contentDescription = null, tint = Color.Gray)
                Spacer(Modifier.width(4.dp))
                Text("Duration: ${session.duration}")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Straighten, contentDescription = null, tint = Color.Gray)
                Spacer(Modifier.width(4.dp))
                Text("Distance: ${session.distance} m")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.Red)
                Spacer(Modifier.width(4.dp))
                Text("Calories: ${session.calories} kcal")
            }
        }
    }
}

fun formatTime(time: String): String {
    return try {
        val sdf24 = SimpleDateFormat("HH:mm", Locale.getDefault())
        val sdf12 = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf12.format(sdf24.parse(time)!!)
    } catch (e: Exception) {
        time
    }
}

data class WalkSession(
    val date: String,
    val startTime: String,
    val endTime: String,
    val duration: String,
    val distance: Int,
    val calories: Int
)

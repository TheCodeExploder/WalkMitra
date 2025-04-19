@file:OptIn(ExperimentalMaterial3Api::class)

package com.prashant.walkmitra.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

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
    }.sortedByDescending { it.date + it.startTime } // newest first

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
            if (sessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No walk data yet.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sessions) { session ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Date: ${session.date}", style = MaterialTheme.typography.titleMedium)
                                Text("Start: ${formatTime(session.startTime)} | End: ${formatTime(session.endTime)}")
                                Text("Duration: ${session.duration}")
                                Text("Distance: ${session.distance} m")
                                Text("Calories: ${session.calories} kcal")
                            }
                        }
                    }
                }
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
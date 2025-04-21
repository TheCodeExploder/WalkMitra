@file:OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)

package com.prashant.walkmitra.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.accompanist.permissions.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.prashant.walkmitra.data.UserProfile
import com.prashant.walkmitra.location.LocationService
import com.prashant.walkmitra.location.LocationUpdateManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun MainScreen(navController: NavController, userProfile: UserProfile?) {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val serviceIntent = remember { Intent(context, LocationService::class.java) }
    val cameraPositionState = rememberCameraPositionState()
    val scope = rememberCoroutineScope()

    var isTracking by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var distance by remember { mutableStateOf(0.0) }
    var speed by remember { mutableStateOf(0.0) }
    var calories by remember { mutableStateOf(0.0) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var timerStartTime by remember { mutableStateOf(0L) }
    var timerJob by remember { mutableStateOf<Job?>(null) }
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    var lastTime by remember { mutableStateOf(0L) }
    var pathPoints by remember { mutableStateOf(listOf<LatLng>()) }

    val sharedPreferences = context.getSharedPreferences("walkmitra_history", Context.MODE_PRIVATE)

    val mapUiSettings = remember { MapUiSettings(zoomControlsEnabled = false) }
    val mapProperties by rememberUpdatedState(MapProperties(isMyLocationEnabled = true))

    fun formatDuration(ms: Long): String {
        val h = ms / 3600000
        val m = (ms / 60000) % 60
        val s = (ms / 1000) % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    fun startTimer() {
        timerStartTime = System.currentTimeMillis() - elapsedTime
        timerJob = scope.launch {
            while (true) {
                elapsedTime = System.currentTimeMillis() - timerStartTime
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun stopTimer() = timerJob?.cancel()

    fun resetSession() {
        stopTimer()
        distance = 0.0
        speed = 0.0
        calories = 0.0
        elapsedTime = 0L
        pathPoints = emptyList()
        lastLocation = null
        lastTime = 0L
    }

    fun saveSession() {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val startTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timerStartTime))
        val endTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timerStartTime + elapsedTime))
        val duration = formatDuration(elapsedTime)
        val sessionJson = """{"date":"$date","startTime":"$startTime","endTime":"$endTime","duration":"$duration","distance":${distance.roundToInt()},"calories":${calories.roundToInt()}}"""
        val sessions = sharedPreferences.getStringSet("sessions", mutableSetOf())!!.toMutableSet()
        sessions.add(sessionJson)
        sharedPreferences.edit().putStringSet("sessions", sessions).apply()
    }

    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WalkMitra") },
                actions = {
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                    IconButton(onClick = { navController.navigate("history") }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x99FFFFFF), Color(0xFF292D3E))
                    )
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("👋 Hello ${userProfile?.name ?: "Walker"}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Let's make today count!", fontSize = 16.sp)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f),
                shape = RoundedCornerShape(16.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = mapUiSettings,
                    properties = mapProperties
                ) {
                    if (pathPoints.isNotEmpty()) {
                        val color = when {
                            speed < 3 -> Color.Blue
                            speed < 6 -> Color.Green
                            else -> Color.Red
                        }
                        Polyline(points = pathPoints, color = color, width = 16f)
                        Marker(
                            state = MarkerState(position = pathPoints.last()),
                            icon = BitmapDescriptorFactory.defaultMarker(),
                            title = "You",
                            snippet = "Current Location"
                        )
                    }
                }
            }

            StatCardRow(distance, speed, calories)

            Text(
                "Time: ${formatDuration(elapsedTime)}",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                when {
                    !isTracking -> {
                        Button(onClick = {
                            isTracking = true
                            isPaused = false
                            startTimer()
                            ContextCompat.startForegroundService(context, serviceIntent)
                            LocationUpdateManager.setCallback { location ->
                                val latLng = LatLng(location.latitude, location.longitude)
                                pathPoints = pathPoints + latLng
                                scope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
                                }
                                val now = System.currentTimeMillis()
                                lastLocation?.let {
                                    val result = FloatArray(1)
                                    Location.distanceBetween(it.latitude, it.longitude, location.latitude, location.longitude, result)
                                    distance += result[0]
                                }
                                speed = if (elapsedTime > 0) (distance / 1000.0) / (elapsedTime / 60000.0) else 0.0
                                calories = calculateCaloriesFromWalk(distance, userProfile?.weightKg?.toDouble() ?: 70.0)

                                lastLocation = location
                                lastTime = now
                            }
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Text("Start")
                        }
                    }
                    isTracking && !isPaused -> {
                        Button(onClick = {
                            isPaused = true
                            stopTimer()
                            context.stopService(serviceIntent)
                        }) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Text("Pause")
                        }
                        Button(onClick = {
                            isTracking = false
                            stopTimer()
                            context.stopService(serviceIntent)
                            saveSession()
                            resetSession()
                            navController.navigate("history")
                        }) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Text("Stop")
                        }
                    }
                    isTracking && isPaused -> {
                        Button(onClick = {
                            isPaused = false
                            startTimer()
                            ContextCompat.startForegroundService(context, serviceIntent)
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Text("Resume")
                        }
                        Button(onClick = {
                            isTracking = false
                            stopTimer()
                            context.stopService(serviceIntent)
                            saveSession()
                            resetSession()
                            navController.navigate("history")
                        }) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Text("Stop")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(icon: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
fun StatCardRow(distance: Double, speed: Double, calories: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("📏", "%.2f m".format(distance), Modifier.weight(1f))
        StatCard("⚡", "%.2f km/h".format(speed), Modifier.weight(1f))
        StatCard("🔥", "%.2f kcal".format(calories), Modifier.weight(1f))
    }
}

fun calculateCaloriesFromWalk(distanceMeters: Double, weightKg: Double): Double {
    val distanceKm = distanceMeters / 1000.0
    val met = 3.5
    return ((met * weightKg * (distanceKm / 5.0)) * 100).roundToInt() / 100.0
}

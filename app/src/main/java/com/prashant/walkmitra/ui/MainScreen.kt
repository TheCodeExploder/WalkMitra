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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.accompanist.permissions.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.MapView
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
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var isFirstLocation by remember { mutableStateOf(true) }

    val sharedPreferences = context.getSharedPreferences("walkmitra_history", Context.MODE_PRIVATE)

    fun formatDuration(ms: Long): String {
        val h = ms / 3600000
        val m = (ms / 60000) % 60
        val s = (ms / 1000) % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
    fun clearPersistedSession() {
        sharedPreferences.edit()
            .remove("isTracking")
            .remove("isPaused")
            .remove("elapsedTime")
            .remove("timerStartTime")
            .remove("distance")
            .remove("speed")
            .remove("calories")
            .remove("pathPoints")
            .apply()
    }
    fun startTimer() {
        timerStartTime = System.currentTimeMillis() - elapsedTime
        timerJob = scope.launch {
            var tickCount = 0
            while (true) {
                elapsedTime = System.currentTimeMillis() - timerStartTime
                tickCount++
                if (tickCount % 10 == 0) {
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("isTracking", isTracking)
                    editor.putBoolean("isPaused", isPaused)
                    editor.putLong("elapsedTime", elapsedTime)
                    editor.putLong("timerStartTime", timerStartTime)
                    editor.putFloat("distance", distance.toFloat())
                    editor.putFloat("speed", speed.toFloat())
                    editor.putFloat("calories", calories.toFloat())
                    editor.putString("pathPoints", pathPoints.joinToString(";") { "${it.latitude},${it.longitude}" })
                    editor.apply()
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun resetSession() {
        stopTimer()
        distance = 0.0
        speed = 0.0
        calories = 0.0
        elapsedTime = 0L
        pathPoints = emptyList()
        lastLocation = null
        lastTime = 0L
        isFirstLocation = true
        clearPersistedSession()
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
        if (sharedPreferences.getBoolean("isTracking", false)) {
            isTracking = true
            isPaused = sharedPreferences.getBoolean("isPaused", false)
            elapsedTime = sharedPreferences.getLong("elapsedTime", 0L)
            timerStartTime = sharedPreferences.getLong("timerStartTime", System.currentTimeMillis() - elapsedTime)
            distance = sharedPreferences.getFloat("distance", 0f).toDouble()
            speed = sharedPreferences.getFloat("speed", 0f).toDouble()
            calories = sharedPreferences.getFloat("calories", 0f).toDouble()
            val pathString = sharedPreferences.getString("pathPoints", "") ?: ""
            try {
                pathPoints = pathString.split(";").filter { it.contains(",") }.map {
                    val (lat, lng) = it.split(",")
                    LatLng(lat.toDouble(), lng.toDouble())
                }
            } catch (e: Exception) {
                pathPoints = emptyList()
            }
            if (!isPaused) startTimer()
        }
    }

    LaunchedEffect(isTracking) {
        if (isTracking) {
            LocationUpdateManager.setCallback { location ->
                val latLng = LatLng(location.latitude, location.longitude)
                if (isFirstLocation && latLng.latitude != 0.0 && latLng.longitude != 0.0) {
                    mapView?.getMapAsync { map ->
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
                    }
                    isFirstLocation = false
                }
                pathPoints = pathPoints + latLng
                mapView?.getMapAsync { map ->
                    map.clear()
                    map.addPolyline(
                        PolylineOptions()
                            .addAll(pathPoints)
                            .color(android.graphics.Color.BLUE)
                            .width(10f)
                    )
                }
                lastLocation?.let {
                    val result = FloatArray(1)
                    Location.distanceBetween(it.latitude, it.longitude, location.latitude, location.longitude, result)
                    distance += result[0]
                }
                speed = if (elapsedTime > 0) (distance / 1000.0) / (elapsedTime / 60000.0) else 0.0
                calories = calculateCaloriesFromWalk(distance, userProfile?.weightKg?.toDouble() ?: 70.0)
                lastLocation = location
                lastTime = System.currentTimeMillis()
            }
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
                .background(Brush.verticalGradient(listOf(Color(0xFFB3E5FC), Color(0xFFFFE0B2))))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEEBAA)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("👋 Hello ${userProfile?.name ?: "Walker"}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Let's make today count!", fontSize = 16.sp)
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f),
                shape = RoundedCornerShape(16.dp)
            ) {
                AndroidView(factory = {
                    MapView(context).apply {
                        onCreate(null)
                        onResume()
                        mapView = this
                        getMapAsync { map ->
                            map.uiSettings.isZoomControlsEnabled = false
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                map.isMyLocationEnabled = true
                            }
                        }
                    }
                }, modifier = Modifier.fillMaxSize())
            }
            StatCardRow(distance, speed, calories)

            Text(
                text = formatDuration(elapsedTime),
                fontFamily = FontFamily.Monospace,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0FFF00),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .background(Color.Black, shape = RoundedCornerShape(2.dp))
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                when {
                    !isTracking -> {
                        Button(onClick = {
                            isTracking = true
                            isPaused = false
                            startTimer()
                            ContextCompat.startForegroundService(context, serviceIntent)
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
        modifier = modifier
            .height(80.dp)
            .padding(4.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0B2)) // light cyan-blue
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}


@Composable
fun StatCardRow(distance: Double, speed: Double, calories: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("🚶‍♂️", "%.2f m".format(distance), Modifier.weight(1f))
        StatCard("🏁", "%.2f km/h".format(speed), Modifier.weight(1f))
        StatCard("🔥", "%.2f kcal".format(calories), Modifier.weight(1f))
    }
}

fun calculateCaloriesFromWalk(distanceMeters: Double, weightKg: Double): Double {
    val distanceKm = distanceMeters / 1000.0
    val met = 3.5
    return ((met * weightKg * (distanceKm / 5.0)) * 100).roundToInt() / 100.0
}

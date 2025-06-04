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
import androidx.compose.ui.draw.scale
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.android.gms.maps.model.CameraPosition
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import java.io.File
import java.io.FileOutputStream
import android.graphics.Bitmap
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.prashant.walkmitra.ui.BouncyButton
import com.prashant.walkmitra.ui.GpsSignalIndicator

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
                        map.animateCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder()
                                    .target(latLng)
                                    .zoom(18f)
                                    .tilt(0f) // 0° = 2D flat view
                                    .bearing(0f)
                                    .build()
                            )
                        )

                    }
                    isFirstLocation = false
                }
                pathPoints = pathPoints + latLng
                mapView?.getMapAsync { map ->
                    map.clear()
                    map.addPolyline(
                        PolylineOptions()
                            .addAll(pathPoints)
                            .color(android.graphics.Color.GREEN)
                            .width(20f)
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
            GpsSignalIndicator(isGpsActive = isTracking && !isPaused)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
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
                    .height(300.dp)
                    .padding(horizontal = 12.dp) // ✅ Apply padding here
                    .shadow(8.dp, RoundedCornerShape(20.dp)), // Optional: Stronger 3D feel
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp)

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
            StatCardRow(distance, speed, calories, isTracking)

            Card(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp)
                    .height(50.dp)
                    .widthIn(min = 150.dp)
                    .graphicsLayer {
                        shadowElevation = 12.dp.toPx()
                        shape = RoundedCornerShape(11.dp)
                        clip = true
                    },
                elevation = CardDefaults.cardElevation(12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFF8F8F8),
                                    Color(0xFFDDDDDD)
                                )
                            )
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatDuration(elapsedTime),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
            }


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                if (!isTracking) {
                    BouncyButton(
                        onClick = {
                            isTracking = true
                            isPaused = false
                            startTimer()
                            ContextCompat.startForegroundService(context, serviceIntent)
                        },
                        color = Color(0xFF4CAF50), // Green
                        icon = Icons.Default.PlayArrow,
                        text = "Start"
                    )
                } else if (isTracking && !isPaused) {
                    BouncyButton(
                        onClick = {
                            isPaused = true
                            stopTimer()
                            context.stopService(serviceIntent)
                        },
                        color = Color(0xFFFF9800), // Orange
                        icon = Icons.Default.Pause,
                        text = "Pause"
                    )
                    BouncyButton(
                        onClick = {
                            isTracking = false
                            stopTimer()
                            context.stopService(serviceIntent)
                            scope.launch {
                                val path = captureMapScreenshot(mapView, context) ?: ""
                                saveWalkSession(context, distance, elapsedTime, path)
                                resetSession()
                                navController.navigate("history")
                            }
                        },
                        color = Color(0xFFF44336), // Red
                        icon = Icons.Default.Stop,
                        text = "Stop"
                    )
                } else if (isTracking && isPaused) {
                    BouncyButton(
                        onClick = {
                            isPaused = false
                            startTimer()
                            ContextCompat.startForegroundService(context, serviceIntent)
                        },
                        color = Color(0xFF4CAF50), // Green
                        icon = Icons.Default.PlayArrow,
                        text = "Resume"
                    )
                    BouncyButton(
                        onClick = {
                            isTracking = false
                            stopTimer()
                            context.stopService(serviceIntent)
                            scope.launch {
                                val path = captureMapScreenshot(mapView, context) ?: ""
                                saveWalkSession(context, distance, elapsedTime, path)
                                resetSession()
                                navController.navigate("history")
                            }
                        },
                        color = Color(0xFFF44336), // Red
                        icon = Icons.Default.Stop,
                        text = "Stop"
                    )
                }
            }

        }
    }
}

@Composable
fun StatCard(icon: String, value: String, modifier: Modifier = Modifier, isTracking: Boolean) {
    val pulseScale by animateFloatAsState(
        targetValue = if (isTracking) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            }
            .height(80.dp)
            .padding(4.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0B2))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
fun StatCardRow(distance: Double, speed: Double, calories: Double, isTracking: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("🚶‍♂️", "%.2f m".format(distance), Modifier.weight(1f), isTracking)
        StatCard("🏁", "%.2f km/h".format(speed), Modifier.weight(1f), isTracking)
        StatCard("🔥", "%.2f kcal".format(calories), Modifier.weight(1f), isTracking)
    }
}


fun calculateCaloriesFromWalk(distanceMeters: Double, weightKg: Double): Double {
    val distanceKm = distanceMeters / 1000.0
    val met = 3.5
    return ((met * weightKg * (distanceKm / 5.0)) * 100).roundToInt() / 100.0
}

suspend fun captureMapScreenshot(mapView: MapView?, context: Context): String? =
    suspendCancellableCoroutine { cont ->
        if (mapView == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        mapView.getMapAsync { map ->
            map.snapshot { bmp ->
                try {
                    val file = File(context.filesDir, "walk_screenshot.png")
                    FileOutputStream(file).use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    cont.resume(file.absolutePath)
                } catch (e: Exception) {
                    cont.resume(null)
                }
            }
        }
    }

suspend fun saveWalkSession(context: Context, distance: Double, duration: Long, screenshotPath: String) {
    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    val history = WalkHistory(date, distance.toFloat(), duration, screenshotPath)
    saveWalkHistory(context, history)
}

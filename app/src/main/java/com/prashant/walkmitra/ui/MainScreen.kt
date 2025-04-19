package com.prashant.walkmitra.ui

import android.Manifest
import android.content.Context
import android.graphics.*
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.permissions.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.prashant.walkmitra.data.UserProfile
import com.prashant.walkmitra.location.LocationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, userProfile: UserProfile?) {
    val context = LocalContext.current
    val locationService = remember { LocationService(context) }
    val scope = rememberCoroutineScope()

    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    var isTracking by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    var distance by remember { mutableStateOf(0.0) }
    var calories by remember { mutableStateOf(0.0) }
    var speed by remember { mutableStateOf(0.0) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var timerStartTime by remember { mutableStateOf(0L) }
    var timerJob by remember { mutableStateOf<Job?>(null) }

    var lastLocation by remember { mutableStateOf<Location?>(null) }
    var lastTime by remember { mutableStateOf(0L) }
    var pathPoints by remember { mutableStateOf(listOf<LatLng>()) }

    val cameraPositionState = rememberCameraPositionState()
    val sharedPreferences = context.getSharedPreferences("walkmitra_history", 0)

    fun formatDuration(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms / 60000) % 60
        val seconds = (ms / 1000) % 60
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
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

    fun stopTimer() { timerJob?.cancel() }

    fun resetSession() {
        stopTimer()
        distance = 0.0
        calories = 0.0
        speed = 0.0
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
        } else {
            locationService.getCurrentLocation { location ->
                val latLng = LatLng(location.latitude, location.longitude)
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WalkMitra") },
                actions = {
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Default.Person, contentDescription = "Edit Profile")
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
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("👋 Hello ${userProfile?.name ?: "Walker"}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Let's make today count!", fontSize = 16.sp)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .weight(0.5f),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    if (pathPoints.isNotEmpty()) {
                        val color = when {
                            speed < 3 -> Color.Blue
                            speed < 6 -> Color.Green
                            else -> Color.Red
                        }
                        Polyline(
                            points = pathPoints,
                            color = color,
                            width = 16f
                        )
                        val emojiIcon = remember(pathPoints.last()) {
                            emojiToBitmapDescriptor(context, "🚶")
                        }
                        Marker(
                            state = MarkerState(position = pathPoints.last()),
                            icon = emojiIcon,
                            title = null,
                            snippet = null
                        )
                    }
                }
            }

            StatCardRow(distance = distance, speed = speed, calories = calories)
            TimeDisplayCard(elapsedTime = elapsedTime)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                when {
                    !isTracking -> Button(onClick = {
                        isTracking = true
                        isPaused = false
                        startTimer()
                        locationService.startLocationUpdates { location ->
                            val latLng = LatLng(location.latitude, location.longitude)
                            pathPoints = pathPoints + latLng
                            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
                            val currentTime = System.currentTimeMillis()
                            if (lastLocation != null && lastTime != 0L) {
                                val result = FloatArray(1)
                                Location.distanceBetween(
                                    lastLocation!!.latitude, lastLocation!!.longitude,
                                    location.latitude, location.longitude,
                                    result
                                )
                                val deltaTimeHours = (currentTime - lastTime).toDouble() / 3600000.0
                                distance += result[0]
                                speed = if (deltaTimeHours > 0.0) (result[0] / 1000.0) / deltaTimeHours else 0.0
                                calories = calculateCaloriesFromWalk(distance, userProfile?.weightKg?.toDouble() ?: 70.0)
                            }
                            lastLocation = location
                            lastTime = currentTime
                        }
                    }) { Icon(Icons.Default.PlayArrow, contentDescription = null); Text("Start") }

                    isTracking && !isPaused -> {
                        Button(onClick = {
                            isPaused = true
                            locationService.stopLocationUpdates()
                            stopTimer()
                        }) { Icon(Icons.Default.Pause, contentDescription = null); Text("Pause") }
                        Button(onClick = {
                            isTracking = false
                            isPaused = false
                            locationService.stopLocationUpdates()
                            stopTimer()
                            saveSession()
                            resetSession()
                        }) { Icon(Icons.Default.Stop, contentDescription = null); Text("Stop") }
                    }

                    isTracking && isPaused -> {
                        Button(onClick = {
                            isPaused = false
                            startTimer()
                            locationService.startLocationUpdates { location ->
                                val latLng = LatLng(location.latitude, location.longitude)
                                pathPoints = pathPoints + latLng
                                scope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
                                }
                                val currentTime = System.currentTimeMillis()
                                if (lastLocation != null && lastTime != 0L) {
                                    val result = FloatArray(1)
                                    Location.distanceBetween(
                                        lastLocation!!.latitude, lastLocation!!.longitude,
                                        location.latitude, location.longitude,
                                        result
                                    )
                                    val deltaTimeHours = (currentTime - lastTime).toDouble() / 3600000.0
                                    distance += result[0]
                                    speed = if (deltaTimeHours > 0.0) (result[0] / 1000.0) / deltaTimeHours else 0.0
                                    calories = calculateCaloriesFromWalk(distance, userProfile?.weightKg?.toDouble() ?: 70.0)
                                }
                                lastLocation = location
                                lastTime = currentTime
                            }
                        }) { Icon(Icons.Default.PlayArrow, contentDescription = null); Text("Resume") }
                        Button(onClick = {
                            isTracking = false
                            isPaused = false
                            locationService.stopLocationUpdates()
                            stopTimer()
                            saveSession()
                            resetSession()
                        }) { Icon(Icons.Default.Stop, contentDescription = null); Text("Stop") }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(icon: String, value: String) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(72.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
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
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun StatCardRow(distance: Double, speed: Double, calories: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatCard(icon = "📏", value = "%.2f m".format(distance))
        StatCard(icon = "🏃‍♂️", value = "%.2f km/h".format(speed))
        StatCard(icon = "🔥", value = "%.2f kcal".format(calories))
    }
}

@Composable
fun Color.darken(factor: Float): Color {
    val r = (red * (1 - factor)).coerceIn(0f, 1f)
    val g = (green * (1 - factor)).coerceIn(0f, 1f)
    val b = (blue * (1 - factor)).coerceIn(0f, 1f)
    return Color(r, g, b)
}

@Composable
fun TimeDisplayCard(elapsedTime: Long) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Time: %02d:%02d:%02d".format(
                    elapsedTime / 3600000,
                    (elapsedTime / 60000) % 60,
                    (elapsedTime / 1000) % 60
                ),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

fun emojiToBitmapDescriptor(context: Context, emoji: String): BitmapDescriptor {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.textSize = 64f
    paint.color = android.graphics.Color.BLACK
    paint.textAlign = Paint.Align.LEFT

    val baseline = -paint.ascent()
    val width = (paint.measureText(emoji) + 0.5f).toInt()
    val height = (baseline + paint.descent() + 0.5f).toInt()

    val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(image)
    canvas.drawText(emoji, 0f, baseline, paint)

    return BitmapDescriptorFactory.fromBitmap(image)
}

fun calculateCaloriesFromWalk(distanceMeters: Double, weightKg: Double): Double {
    val distanceKm = distanceMeters / 1000.0
    val met = 3.5
    return ((met * weightKg * (distanceKm / 5.0)) * 100).roundToInt() / 100.0
}

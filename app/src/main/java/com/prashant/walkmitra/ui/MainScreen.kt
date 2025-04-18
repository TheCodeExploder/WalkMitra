package com.prashant.walkmitra.ui

import android.Manifest
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.prashant.walkmitra.data.UserProfile
import com.prashant.walkmitra.location.LocationService
import kotlinx.coroutines.launch
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

    var lastLocation by remember { mutableStateOf<Location?>(null) }
    var lastTime by remember { mutableStateOf(0L) }
    var pathPoints by remember { mutableStateOf(listOf<LatLng>()) }

    val cameraPositionState = rememberCameraPositionState()

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    if (pathPoints.isNotEmpty()) {
                        Polyline(
                            points = pathPoints,
                            color = Color(0xFF1E88E5),
                            width = 8f
                        )
                        Marker(
                            state = MarkerState(position = pathPoints.last()),
                            title = "You",
                            snippet = "Current Location"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("Distance: %.2f meters".format(distance))
                Text("Speed: %.2f km/h".format(speed))
                Text("Calories Burned: %.2f kcal".format(calories))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        !isTracking -> {
                            Button(onClick = {
                                isTracking = true
                                isPaused = false
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
                                        calories = calculateCalories(distance, userProfile?.weightKg?.toDouble() ?: 70.0)
                                    }
                                    lastLocation = location
                                    lastTime = currentTime
                                }
                            }) {
                                Text("Start")
                            }
                        }

                        isTracking && !isPaused -> {
                            Button(onClick = {
                                isPaused = true
                                locationService.stopLocationUpdates()
                            }) {
                                Text("Pause")
                            }
                            Button(onClick = {
                                isTracking = false
                                isPaused = false
                                locationService.stopLocationUpdates()
                                distance = 0.0
                                calories = 0.0
                                speed = 0.0
                                pathPoints = emptyList()
                                lastLocation = null
                                lastTime = 0L
                            }) {
                                Text("Stop")
                            }
                        }

                        isTracking && isPaused -> {
                            Button(onClick = {
                                isPaused = false
                                locationService.startLocationUpdates { location ->
                                    val latLng = LatLng(location.latitude, location.longitude)
                                    pathPoints = pathPoints + latLng

                                    scope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(latLng, 18f)
                                        )
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
                                        calories = calculateCalories(distance, userProfile?.weightKg?.toDouble() ?: 70.0)
                                    }
                                    lastLocation = location
                                    lastTime = currentTime
                                }
                            }) {
                                Text("Resume")
                            }
                            Button(onClick = {
                                isTracking = false
                                isPaused = false
                                locationService.stopLocationUpdates()
                                distance = 0.0
                                calories = 0.0
                                speed = 0.0
                                pathPoints = emptyList()
                                lastLocation = null
                                lastTime = 0L
                            }) {
                                Text("Stop")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun calculateCalories(distanceMeters: Double, weightKg: Double): Double {
    val distanceKm = distanceMeters / 1000.0
    val met = 3.5 // average MET for walking ~4-5 km/h
    return ((met * weightKg * (distanceKm / 5.0)) * 100).roundToInt() / 100.0
}

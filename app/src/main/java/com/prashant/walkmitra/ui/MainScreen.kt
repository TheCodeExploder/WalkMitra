@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    com.google.accompanist.permissions.ExperimentalPermissionsApi::class
)

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.prashant.walkmitra.location.LocationService
import kotlin.math.roundToInt
import kotlinx.coroutines.launch


@Composable
fun MainScreen(navController: NavController) {
    val context = LocalContext.current
    val locationService = remember { LocationService(context) }
    val scope = rememberCoroutineScope()

    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    var isTracking by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    var distance by remember { mutableStateOf(0.0) }
    var calories by remember { mutableStateOf(0.0) }

    var lastLocation by remember { mutableStateOf<Location?>(null) }
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
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Google Map
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f),
                cameraPositionState = cameraPositionState
            ) {
                if (pathPoints.isNotEmpty()) {
                    Polyline(
                        points = pathPoints,
                        color = Color(0xFF1E88E5), // ✅ Use Compose color directly

                        width = 8f
                    )
                    Marker(
                        state = MarkerState(position = pathPoints.last()),
                        title = "You",
                        snippet = "Current Location"
                    )
                }
            }

            Text("Distance: %.2f meters".format(distance))
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

                                cameraPositionState.move(
                                    CameraUpdateFactory.newLatLngZoom(latLng, 18f)
                                )


                                if (lastLocation != null) {
                                    val result = FloatArray(1)
                                    Location.distanceBetween(
                                        lastLocation!!.latitude, lastLocation!!.longitude,
                                        location.latitude, location.longitude,
                                        result
                                    )
                                    distance += result[0]
                                    calories = calculateCalories(distance)
                                }
                                lastLocation = location
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
                            pathPoints = emptyList()
                            lastLocation = null
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

                                if (lastLocation != null) {
                                    val result = FloatArray(1)
                                    Location.distanceBetween(
                                        lastLocation!!.latitude, lastLocation!!.longitude,
                                        location.latitude, location.longitude,
                                        result
                                    )
                                    distance += result[0]
                                    calories = calculateCalories(distance)
                                }
                                lastLocation = location
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
                            pathPoints = emptyList()
                            lastLocation = null
                        }) {
                            Text("Stop")
                        }
                    }
                }
            }
        }
    }
}

// 🔥 Simple calorie calculator for now
fun calculateCalories(distanceMeters: Double): Double {
    val distanceKm = distanceMeters / 1000
    val caloriesPerKm = 60.0 // approx. for 70kg person
    return (distanceKm * caloriesPerKm * 100).roundToInt() / 100.0
}

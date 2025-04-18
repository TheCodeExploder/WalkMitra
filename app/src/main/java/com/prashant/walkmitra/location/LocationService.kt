package com.prashant.walkmitra.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.*

class LocationService(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.create().apply {
        interval = 5000 // 5 seconds
        fastestInterval = 2000
        priority = Priority.PRIORITY_HIGH_ACCURACY
    }

    private var callback: ((Location) -> Unit)? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            for (location in result.locations) {
                callback?.invoke(location)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onLocationUpdate: (Location) -> Unit) {
        callback = onLocationUpdate
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(onResult: (Location) -> Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onResult(location)
            }
        }
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}

// MapViewContainer.kt
package com.prashant.walkmitra.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.CompletableDeferred
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
@Composable
fun MapViewContainer(
    modifier: Modifier = Modifier,
    pathPoints: List<LatLng>,
    cameraLatLng: LatLng,
    onMapReady: (MapView, GoogleMap) -> Unit
) {
    val context = LocalContext.current

    AndroidView(
        factory = {
            val mapView = MapView(context)
            mapView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            mapView.onCreate(Bundle())
            mapView.getMapAsync { googleMap ->
                onMapReady(mapView, googleMap)

                googleMap.uiSettings.isZoomControlsEnabled = false
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    googleMap.isMyLocationEnabled = true
                }
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cameraLatLng, 18f))

                if (pathPoints.isNotEmpty()) {
                    googleMap.addPolyline(
                        PolylineOptions()
                            .addAll(pathPoints)
                            .color(0xFF03A9F4.toInt()) // blue
                            .width(12f)
                    )
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(pathPoints.last())
                            .title("You")
                    )
                }
            }
            mapView
        },
        modifier = modifier,
        update = { it.onResume() }
    )
}

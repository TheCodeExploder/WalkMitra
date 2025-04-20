package com.prashant.walkmitra.location

import android.location.Location

object LocationUpdateManager {
    private var callback: ((Location) -> Unit)? = null

    fun setCallback(cb: (Location) -> Unit) {
        callback = cb
    }

    fun sendLocation(location: Location) {
        callback?.invoke(location)
    }
}

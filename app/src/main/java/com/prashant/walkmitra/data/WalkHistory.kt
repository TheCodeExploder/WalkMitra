package com.prashant.walkmitra.data

import kotlinx.serialization.Serializable

@Serializable
data class WalkHistory(
    val date: String,
    val distance: Float,
    val duration: Long,
    val screenshotPath: String
)

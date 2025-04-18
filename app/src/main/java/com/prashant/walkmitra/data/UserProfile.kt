package com.prashant.walkmitra.data
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val name: String,
    val gender: String,
    val yearOfBirth: Int,
    val heightFeet: Int,
    val heightInches: Int,
    val weightKg: Float,
    val bodyType: String
)


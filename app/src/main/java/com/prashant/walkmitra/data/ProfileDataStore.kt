package com.prashant.walkmitra.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// Create the DataStore instance
val Context.profileDataStore by preferencesDataStore(name = "user_profile")

object ProfileKeys {
    val profileJsonKey = stringPreferencesKey("profile_json")
}

suspend fun Context.saveUserProfile(profile: UserProfile) {
    val json = Json.encodeToString(profile)
    profileDataStore.edit { prefs ->
        prefs[ProfileKeys.profileJsonKey] = json
    }
}

suspend fun Context.loadUserProfile(): UserProfile? {
    val prefs = profileDataStore.data.map { it[ProfileKeys.profileJsonKey] }.first()
    return prefs?.let { Json.decodeFromString(it) }
}

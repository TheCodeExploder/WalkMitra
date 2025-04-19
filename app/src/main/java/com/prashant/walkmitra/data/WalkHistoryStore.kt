package com.prashant.walkmitra.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore by preferencesDataStore(name = "walkmitra_datastore")
val historyKey = stringPreferencesKey("walk_history")

suspend fun saveWalkHistory(context: Context, walk: WalkHistory) {
    val json = Json.encodeToString(walk)
    context.dataStore.edit { prefs ->
        val existing = prefs[historyKey] ?: ""
        val updated = if (existing.isEmpty()) json else "$existing||$json"
        prefs[historyKey] = updated
    }
}

suspend fun loadWalkHistories(context: Context): List<WalkHistory> {
    val data = context.dataStore.data.first()
    val raw = data[historyKey] ?: return emptyList()
    return raw.split("||").mapNotNull {
        try {
            Json.decodeFromString<WalkHistory>(it)
        } catch (e: Exception) {
            null
        }
    }
}

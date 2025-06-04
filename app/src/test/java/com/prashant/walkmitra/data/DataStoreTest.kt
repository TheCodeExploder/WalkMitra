package com.prashant.walkmitra.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DataStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun profile_roundTrip() = runBlocking {
        val profile = UserProfile("John", "Male", 1990, 5, 9, 70f, "Average")
        context.saveUserProfile(profile)
        val loaded = context.loadUserProfile()
        assertEquals(profile, loaded)
    }

    @Test
    fun walkHistory_roundTrip() = runBlocking {
        val history = WalkHistory("2024-01-01", 100f, 1000L, "path")
        saveWalkHistory(context, history)
        val loaded = loadWalkHistories(context).last()
        assertEquals(history, loaded)
    }
}

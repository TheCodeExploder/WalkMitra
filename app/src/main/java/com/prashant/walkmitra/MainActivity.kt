package com.prashant.walkmitra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.prashant.walkmitra.data.UserProfile
import com.prashant.walkmitra.data.loadUserProfile
import com.prashant.walkmitra.data.saveUserProfile
import com.prashant.walkmitra.ui.MainScreen
import com.prashant.walkmitra.ui.ProfileScreen
import com.prashant.walkmitra.ui.theme.WalkMitraTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WalkMitraTheme {
                val navController = rememberNavController()
                var loadedProfile by remember { mutableStateOf<UserProfile?>(null) }
                var isLoading by remember { mutableStateOf(true) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    val profile = applicationContext.loadUserProfile()
                    loadedProfile = profile
                    isLoading = false
                    if (profile == null) {
                        navController.navigate("profile") {
                            popUpTo("start") { inclusive = true }
                        }
                    } else {
                        navController.navigate("main") {
                            popUpTo("start") { inclusive = true }
                        }
                    }
                }

                NavHost(navController = navController, startDestination = "start") {
                    composable("start") {
                        if (isLoading) {
                            CircularProgressIndicator()
                        }
                    }

                    composable("profile") {
                        ProfileScreen(navController = navController) { profile ->
                            scope.launch {
                                applicationContext.saveUserProfile(profile)
                                loadedProfile = profile
                                navController.navigate("main") {
                                    popUpTo("profile") { inclusive = true }
                                }
                            }
                        }
                    }

                    composable("main") {
                        MainScreen(navController = navController, userProfile = loadedProfile)
                    }

                    // composable("history") { HistoryScreen() } // Placeholder
                }
            }
        }
    }
}

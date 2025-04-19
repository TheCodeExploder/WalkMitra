package com.prashant.walkmitra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.*
import com.prashant.walkmitra.data.UserProfile
import com.prashant.walkmitra.data.loadUserProfile
import com.prashant.walkmitra.data.saveUserProfile
import com.prashant.walkmitra.ui.MainScreen
import com.prashant.walkmitra.ui.ProfileScreen
import com.prashant.walkmitra.ui.theme.WalkMitraTheme
import com.prashant.walkmitra.ui.HistoryScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WalkMitraTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                var loadedProfile by remember { mutableStateOf<UserProfile?>(null) }
                var isLoading by remember { mutableStateOf(true) }
                val scope = rememberCoroutineScope()

                // Load profile
                LaunchedEffect(Unit) {
                    val profile = context.loadUserProfile()
                    loadedProfile = profile
                    isLoading = false
                    navController.navigate(if (profile == null) "profile" else "main") {
                        popUpTo("start") { inclusive = true }
                    }
                }

                NavHost(navController = navController, startDestination = "start") {
                    composable("start") {
                        CircularProgressIndicator()
                    }

                    composable("profile") {
                        ProfileScreen(navController = navController) { profile ->
                            scope.launch {
                                context.saveUserProfile(profile)
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

                    composable("history") {
                        HistoryScreen(navController = navController, context = context)
                    }
                }
            }
        }
    }
}

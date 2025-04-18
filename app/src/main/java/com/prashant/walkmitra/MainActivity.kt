package com.prashant.walkmitra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
                val context = applicationContext
                var profileLoaded by remember { mutableStateOf(false) }
                var hasProfile by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    val profile = context.loadUserProfile()
                    hasProfile = profile != null
                    profileLoaded = true
                }

                if (!profileLoaded) {
                    CircularProgressIndicator()
                } else {
                    NavHost(
                        navController = navController,
                        startDestination = if (hasProfile) "main" else "profile"
                    ) {
                        composable("profile") {
                            ProfileScreen(navController) { profile ->
                                scope.launch {
                                    context.saveUserProfile(profile)
                                    navController.navigate("main") {
                                        popUpTo("profile") { inclusive = true }
                                    }
                                }
                            }
                        }
                        composable("main") {
                            MainScreen(navController)
                        }
                    }
                }
            }
        }
    }
}

@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    com.google.accompanist.permissions.ExperimentalPermissionsApi::class
)

package com.prashant.walkmitra.ui

import android.Manifest
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.prashant.walkmitra.data.loadUserProfile
import com.prashant.walkmitra.data.UserProfile
import com.prashant.walkmitra.location.LocationService
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(navController: NavController, onSave: (UserProfile) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var yearOfBirth by remember { mutableStateOf("") }
    var heightFeet by remember { mutableStateOf("") }
    var heightInches by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var bodyType by remember { mutableStateOf("") }

    val genderOptions = listOf("Male", "Female", "Other")
    val bodyTypeOptions = listOf("Lean", "Average", "Muscular", "Overweight")
    val feetOptions = (1..10).map { it.toString() }
    val inchesOptions = (0..11).map { it.toString() }
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val yearOptions = (1925..currentYear).map { it.toString() }.reversed()
    val weightOptions = (10..250).map { it.toString() }

    LaunchedEffect(Unit) {
        val savedProfile = context.loadUserProfile()
        savedProfile?.let {
            name = it.name
            gender = it.gender
            yearOfBirth = it.yearOfBirth.toString()
            heightFeet = it.heightFeet.toString()
            heightInches = it.heightInches.toString()
            weight = it.weightKg.toInt().toString()
            bodyType = it.bodyType
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFB3E5FC), Color(0xFFFFE0B2)) // Gradient effect
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Setup Profile", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        DropdownField("Gender", genderOptions, gender, { gender = it }, Modifier.fillMaxWidth())
        DropdownField("Year of Birth", yearOptions, yearOfBirth, { yearOfBirth = it }, Modifier.fillMaxWidth())

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DropdownField("Height (ft)", feetOptions, heightFeet, { heightFeet = it }, Modifier.weight(1f))

        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            DropdownField("Height (in)", inchesOptions, heightInches, { heightInches = it }, Modifier.weight(1f))
        }

        DropdownField("Weight (kg)", weightOptions, weight, { weight = it }, Modifier.fillMaxWidth())
        DropdownField("Body Type", bodyTypeOptions, bodyType, { bodyType = it }, Modifier.fillMaxWidth())

        Button(
            onClick = {
                try {
                    if (gender.isNotBlank() && yearOfBirth.isNotBlank()
                        && heightFeet.isNotBlank() && heightInches.isNotBlank()
                        && weight.isNotBlank() && bodyType.isNotBlank()
                    ) {
                        val profile = UserProfile(
                            name = name,
                            gender = gender,
                            yearOfBirth = yearOfBirth.toInt(),
                            heightFeet = heightFeet.toInt(),
                            heightInches = heightInches.toInt(),
                            weightKg = weight.toFloat(),
                            bodyType = bodyType
                        )
                        onSave(profile)
                        navController.navigate("main") {
                            popUpTo("profile") { inclusive = true }
                        }
                    }
                } catch (_: Exception) {}
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save & Continue")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = modifier.menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
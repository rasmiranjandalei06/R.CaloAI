package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.MainActivity
import com.example.utils.formatOneDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToOnboarding: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val scrollState = rememberScrollState()

    var showResetDialog by remember { mutableStateOf(false) }

    // Forms temp state
    var editMode by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var ageStr by remember { mutableStateOf("") }
    var heightStr by remember { mutableStateOf("") }
    var weightStr by remember { mutableStateOf("") }
    var targetWeightStr by remember { mutableStateOf("") }
    var goalSelected by remember { mutableStateOf("") }
    var activityLevelSelected by remember { mutableStateOf("") }
    var unitSystemSelected by remember { mutableStateOf("Metric") }

    var apiKeyInput by remember { mutableStateOf("") }
    var showApiKeySecret by remember { mutableStateOf(false) }

    // Sync form values once profile is loaded
    LaunchedEffect(userProfile, editMode) {
        val p = userProfile ?: return@LaunchedEffect
        if (!editMode) {
            name = p.name
            ageStr = p.age.toString()
            heightStr = p.heightCm.formatOneDecimal()
            weightStr = p.weightKg.formatOneDecimal()
            targetWeightStr = p.targetWeightKg.formatOneDecimal()
            goalSelected = p.goal
            activityLevelSelected = p.activityLevel
            unitSystemSelected = p.unitSystem
            apiKeyInput = p.geminiApiKey
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Profile Card Header Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (userProfile?.name ?: "U").take(1).uppercase(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            userProfile?.name ?: "Achiever",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Goal: ${userProfile?.goal ?: "Active Lifestyle"}",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }


            // Personal config settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("METABOLIC CONFIGURATION", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                        
                        TextButton(onClick = {
                            if (editMode) {
                                // Save profile changes
                                val age = ageStr.toIntOrNull() ?: 25
                                val height = heightStr.toDoubleOrNull() ?: 175.0
                                val weight = weightStr.toDoubleOrNull() ?: 70.0
                                val target = targetWeightStr.toDoubleOrNull() ?: 65.0

                                if (age < 10 || age > 120) {
                                    android.widget.Toast.makeText(context, "Please enter a valid age (10-120).", android.widget.Toast.LENGTH_SHORT).show()
                                    return@TextButton
                                }
                                if (height < 50.0 || height > 300.0) {
                                    android.widget.Toast.makeText(context, "Please enter a valid height.", android.widget.Toast.LENGTH_SHORT).show()
                                    return@TextButton
                                }
                                if (weight < 20.0 || weight > 500.0) {
                                    android.widget.Toast.makeText(context, "Please enter a valid weight.", android.widget.Toast.LENGTH_SHORT).show()
                                    return@TextButton
                                }

                                viewModel.updateProfile(
                                    name = name,
                                    age = age,
                                    gender = userProfile?.gender ?: "Male",
                                    heightCm = height,
                                    weightKg = weight,
                                    goal = goalSelected,
                                    activityLevel = activityLevelSelected,
                                    unitSystem = unitSystemSelected,
                                    targetWeightKg = target
                                )
                                editMode = false
                            } else {
                                editMode = true
                            }
                        }) {
                            Icon(imageVector = if (editMode) Icons.Default.Save else Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (editMode) "Save" else "Edit")
                        }
                    }

                    if (!editMode) {
                        ProfileStaticRow(label = "Age", value = "${userProfile?.age ?: 25} years")
                        ProfileStaticRow(label = "Height", value = "${userProfile?.heightCm?.formatOneDecimal() ?: "175"} cm")
                        ProfileStaticRow(label = "Current Weight", value = "${userProfile?.weightKg?.formatOneDecimal() ?: "70"} kg")
                        ProfileStaticRow(label = "Target Goal Weight", value = "${userProfile?.targetWeightKg?.formatOneDecimal() ?: "65"} kg")
                        ProfileStaticRow(label = "Goal Preference", value = userProfile?.goal ?: "Fat Loss")
                        ProfileStaticRow(label = "Activity Lifestyle", value = userProfile?.activityLevel ?: "Sedentary")
                    } else {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = ageStr, onValueChange = { ageStr = it }, label = { Text("Age") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = heightStr, onValueChange = { heightStr = it }, label = { Text("Height (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = weightStr, onValueChange = { weightStr = it }, label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = targetWeightStr, onValueChange = { targetWeightStr = it }, label = { Text("Target weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                        
                        Text("Fitness goal selection:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val choices = listOf("Fat Loss", "Muscle Gain", "Maintain Weight")
                        Column {
                            choices.forEach { choice ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { goalSelected = choice }
                                        .padding(4.dp)
                                ) {
                                    RadioButton(selected = goalSelected == choice, onClick = { goalSelected = choice })
                                    Text(choice)
                                }
                            }
                        }
                    }
                }
            }

            // Client settings controls: Light Dark mode configurations
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SYSTEM PREFERENCES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Force Dark Theme")
                        }

                        val activeDark = userProfile?.darkMode ?: false
                        Switch(
                            checked = activeDark,
                            onCheckedChange = { viewModel.updateDarkMode(it) }
                        )
                    }
                }
            }

            // Clean Wipe option
            Button(
                onClick = { showResetDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Factory Reset Data", color = Color.White, fontWeight = FontWeight.Bold)
            }

            // Version parameters
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "NutriTrack AI v2.6.0 (AI Studio Premium)",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        // Reset verification alert context
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Delete All Local Records?") },
                text = { Text("Warning: This will permanently delete your user profile, weight target history, logged foods, workouts and calorie progress. This cannot be undone.", color = Color.Red) },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        onClick = {
                            viewModel.resetAllData()
                            showResetDialog = false
                            onNavigateToOnboarding()
                        }
                    ) {
                        Text("Confirm Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileStaticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

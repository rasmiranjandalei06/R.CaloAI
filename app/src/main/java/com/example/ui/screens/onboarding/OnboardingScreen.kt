package com.example.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NutriTrackApp
import com.example.data.entities.UserProfile
import com.example.utils.CalorieCalculator
import com.example.utils.CalculationResults
import com.example.utils.formatOneDecimal
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as NutriTrackApp).repository
    val scope = rememberCoroutineScope()

    var currentPage by remember { mutableStateOf(1) }

    // --- State variables for inputs ---
    var name by remember { mutableStateOf("") }
    var ageStr by remember { mutableStateOf("25") }
    var gender by remember { mutableStateOf("Male") } // Male, Female, Other
    var unitSystem by remember { mutableStateOf("Metric") } // Metric or Imperial
    var heightStr by remember { mutableStateOf("175") } // cm or ft
    var weightStr by remember { mutableStateOf("70") } // kg or lbs
    var goal by remember { mutableStateOf("Fat Loss") }
    var activityLevel by remember { mutableStateOf("Moderately Active") }
    var targetWeightStr by remember { mutableStateOf("65") }

    // Body fat US Navy estimator inputs (optional)
    var waistCmStr by remember { mutableStateOf("") }
    var neckCmStr by remember { mutableStateOf("") }
    var hipCmStr by remember { mutableStateOf("") }
    var showBodyFatCalc by remember { mutableStateOf(false) }
    var bodyFatResult by remember { mutableStateOf<Double?>(null) }

    val age = ageStr.toIntOrNull() ?: 25
    val heightCm = if (unitSystem == "Metric") {
        heightStr.toDoubleOrNull() ?: 175.0
    } else {
        // Imperial height: ft to cm (approx)
        val ft = heightStr.toDoubleOrNull() ?: 5.7
        ft * 30.48
    }
    val weightKg = if (unitSystem == "Metric") {
        weightStr.toDoubleOrNull() ?: 70.0
    } else {
        // Imperial weight: lbs to kg (approx)
        val lbs = weightStr.toDoubleOrNull() ?: 154.0
        lbs * 0.45359237
    }

    val calculatedValues = remember(age, gender, heightCm, weightKg, goal, activityLevel) {
        CalorieCalculator.calculateEverything(age, gender, heightCm, weightKg, goal, activityLevel)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header showing step indicators
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepIndicator(num = 1, active = currentPage >= 1, label = "Profile")
                Spacer(modifier = Modifier.width(8.dp))
                Divider(modifier = Modifier.width(32.dp).height(2.dp), color = if (currentPage >= 2) MaterialTheme.colorScheme.primary else Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                StepIndicator(num = 2, active = currentPage >= 2, label = "Goal")
                Spacer(modifier = Modifier.width(8.dp))
                Divider(modifier = Modifier.width(32.dp).height(2.dp), color = if (currentPage >= 3) MaterialTheme.colorScheme.primary else Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                StepIndicator(num = 3, active = currentPage >= 3, label = "Summary")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { width -> width } + fadeIn() with
                                    slideOutHorizontally { width -> -width } + fadeOut()
                        } else {
                            slideInHorizontally { width -> -width } + fadeIn() with
                                    slideOutHorizontally { width -> width } + fadeOut()
                        }.using(
                            SizeTransform(clip = false)
                        )
                    }
                ) { page ->
                    when (page) {
                        1 -> PagePersonalInfo(
                            name = name,
                            onNameChange = { name = it },
                            age = ageStr,
                            onAgeChange = { ageStr = it },
                            gender = gender,
                            onGenderChange = { gender = it },
                            unitSystem = unitSystem,
                            onUnitSystemChange = { unitSystem = it },
                            height = heightStr,
                            onHeightChange = { heightStr = it },
                            weight = weightStr,
                            onWeightChange = { weightStr = it }
                        )
                        2 -> PageGoalSelection(
                            goal = goal,
                            onGoalChange = { goal = it },
                            activityLevel = activityLevel,
                            onActivityLevelChange = { activityLevel = it },
                            targetWeight = targetWeightStr,
                            onTargetWeightChange = { targetWeightStr = it },
                            showBodyFatCalc = showBodyFatCalc,
                            onShowBodyFatCalcChange = { showBodyFatCalc = it },
                            waist = waistCmStr,
                            onWaistChange = { waistCmStr = it },
                            neck = neckCmStr,
                            onNeckChange = { neckCmStr = it },
                            hip = hipCmStr,
                            onHipChange = { hipCmStr = it },
                            bodyFat = bodyFatResult,
                            onCalculateBodyFat = {
                                val waist = waistCmStr.toDoubleOrNull() ?: 0.0
                                val neck = neckCmStr.toDoubleOrNull() ?: 0.0
                                val hip = hipCmStr.toDoubleOrNull() ?: 0.0
                                bodyFatResult = CalorieCalculator.estimateBodyFat(waist, neck, hip, heightCm, gender)
                            }
                        )
                        3 -> PageCalculatedSummary(
                            results = calculatedValues,
                            unitSystem = unitSystem,
                            bodyFat = bodyFatResult
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Actions at bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage > 1) {
                    OutlinedButton(
                        onClick = { currentPage-- },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Button(
                    onClick = {
                        if (currentPage < 3) {
                            if (currentPage == 1) {
                                val inputAge = ageStr.toIntOrNull()
                                if (inputAge == null || inputAge < 10 || inputAge > 120) {
                                    android.widget.Toast.makeText(context, "Please enter a valid age (10-120).", android.widget.Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (heightCm < 50 || heightCm > 300) {
                                    android.widget.Toast.makeText(context, "Please enter a valid height.", android.widget.Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (weightKg < 20 || weightKg > 500) {
                                    android.widget.Toast.makeText(context, "Please enter a valid weight.", android.widget.Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (name.isBlank()) {
                                    name = "Guest User"
                                }
                            }
                            currentPage++
                        } else {
                            // Finish and save UserProfile
                            scope.launch {
                                val initialProfile = UserProfile(
                                    name = name.ifBlank { "Achiever" },
                                    age = age,
                                    gender = gender,
                                    heightCm = heightCm,
                                    weightKg = weightKg,
                                    goal = goal,
                                    activityLevel = activityLevel,
                                    targetCalories = calculatedValues.targetCalories,
                                    targetProtein = calculatedValues.proteinGrams,
                                    targetCarbs = calculatedValues.carbsGrams,
                                    targetFat = calculatedValues.fatGrams,
                                    waterTargetMl = calculatedValues.waterMl,
                                    unitSystem = unitSystem,
                                    targetWeightKg = targetWeightStr.toDoubleOrNull() ?: 70.0
                                )
                                repository.saveUserProfile(initialProfile)
                                onOnboardingComplete()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (currentPage == 3) "Start Tracking →" else "Continue")
                }
            }
        }
    }
}

@Composable
fun StepIndicator(num: Int, active: Boolean, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (active) MaterialTheme.colorScheme.primary
                    else Color.Gray.copy(alpha = 0.5f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = num.toString(),
                color = if (active) MaterialTheme.colorScheme.onPrimary else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (active) MaterialTheme.colorScheme.primary else Color.Gray,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PagePersonalInfo(
    name: String,
    onNameChange: (String) -> Unit,
    age: String,
    onAgeChange: (String) -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit,
    unitSystem: String,
    onUnitSystemChange: (String) -> Unit,
    height: String,
    onHeightChange: (String) -> Unit,
    weight: String,
    onWeightChange: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Welcome to NutriTrack AI!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Let's personalize your metabolic plan",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Full Name") },
            placeholder = { Text("John Doe") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = age,
                onValueChange = onAgeChange,
                label = { Text("Age (years)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Unit Preference",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (unitSystem == "Metric") MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onUnitSystemChange("Metric") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Metric",
                            color = if (unitSystem == "Metric") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (unitSystem == "Imperial") MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onUnitSystemChange("Imperial") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Imperial",
                            color = if (unitSystem == "Imperial") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Column {
            Text(
                text = "Gender",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Male", "Female", "Other").forEach { item ->
                    val isSelected = gender == item
                    OutlinedButton(
                        onClick = { onGenderChange(item) },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = item,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = height,
                onValueChange = onHeightChange,
                label = { Text("Height (${if (unitSystem == "Metric") "cm" else "ft"})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = weight,
                onValueChange = onWeightChange,
                label = { Text("Weight (${if (unitSystem == "Metric") "kg" else "lbs"})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PageGoalSelection(
    goal: String,
    onGoalChange: (String) -> Unit,
    activityLevel: String,
    onActivityLevelChange: (String) -> Unit,
    targetWeight: String,
    onTargetWeightChange: (String) -> Unit,
    showBodyFatCalc: Boolean,
    onShowBodyFatCalcChange: (Boolean) -> Unit,
    waist: String,
    onWaistChange: (String) -> Unit,
    neck: String,
    onNeckChange: (String) -> Unit,
    hip: String,
    onHipChange: (String) -> Unit,
    bodyFat: Double?,
    onCalculateBodyFat: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Select your Fitness Goal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        val goals = listOf(
            Triple("🔥 Fat Loss", "Burn fat, feel lighter", "Fat Loss"),
            Triple("💪 Muscle Gain", "Build strength & size", "Muscle Gain"),
            Triple("⚖️ Maintain Weight", "Stay where you are", "Maintain Weight")
        )

        goals.forEach { (emoji, desc, key) ->
            val isSelected = goal == key
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGoalChange(key) },
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(emoji, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        OutlinedTextField(
            value = targetWeight,
            onValueChange = onTargetWeightChange,
            label = { Text("Target Goal Weight") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Activity Level",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        val activities = listOf(
            "Sedentary" to "Desk job, little to no exercise",
            "Lightly Active" to "1-3 days/week exercise",
            "Moderately Active" to "3-5 days/week exercise",
            "Very Active" to "6-7 days/week hard workout",
            "Extremely Active" to "Athlete, hard physical labor job"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activities.forEach { (title, subtitle) ->
                val isSelected = activityLevel == title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .clickable { onActivityLevelChange(title) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isSelected, onClick = { onActivityLevelChange(title) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(subtitle, fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Body Fat Calculator Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showBodyFatCalc, onCheckedChange = onShowBodyFatCalcChange)
                    Text("Navy Body Fat % Estimator (Optional)", fontWeight = FontWeight.Bold)
                }

                if (showBodyFatCalc) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = waist,
                        onValueChange = onWaistChange,
                        label = { Text("Waist Circumference (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = neck,
                        onValueChange = onNeckChange,
                        label = { Text("Neck Circumference (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = hip,
                        onValueChange = onHipChange,
                        label = { Text("Hip Circumference (cm, females only)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onCalculateBodyFat, modifier = Modifier.align(Alignment.End)) {
                        Text("Calculate %")
                    }

                    if (bodyFat != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Estimated Body Fat: ${bodyFat.formatOneDecimal()}%",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PageCalculatedSummary(
    results: CalculationResults,
    unitSystem: String,
    bodyFat: Double?
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Your Personalized AI Plan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // BMI Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("BMI CALCULATION", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = results.bmi.formatOneDecimal(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = getBmiColor(results.bmiCategory)
                        )
                        Text(
                            text = "Category: ${results.bmiCategory}",
                            fontWeight = FontWeight.Bold,
                            color = getBmiColor(results.bmiCategory)
                        )
                    }

                    // Simulated meter/arc
                    Box(
                        modifier = Modifier
                            .size(100.dp, 20.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Blue, Color.Green, Color.Yellow, Color.Red)
                                )
                            )
                    )
                }
            }
        }

        // Daily Calorie Budget Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("DAILY TARGET BUDGET", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${results.targetCalories} kcal",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Recommended Daily Budget for your Goal",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Estimated TDEE: ${results.tdee.roundToInt()} kcal",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Macro Pie / Bar breakdowns
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("MACRO TARGET DISTRIBUTION", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                MacroProgressLine(name = "Protein", grams = results.proteinGrams, ratio = 0.4f, tint = Color(0xFFFF5722))
                Spacer(modifier = Modifier.height(8.dp))
                MacroProgressLine(name = "Carbs", grams = results.carbsGrams, ratio = 0.4f, tint = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.height(8.dp))
                MacroProgressLine(name = "Fats", grams = results.fatGrams, ratio = 0.2f, tint = Color(0xFFFFEB3B))
            }
        }

        // Water Glass Target Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("WATER TARGET", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1976D2))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${results.waterMl.roundToInt()} ml (${results.waterGlasses} Glasses)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D47A1)
                    )
                    Text("250ml per glass recommendation", fontSize = 11.sp, color = Color.Gray)
                }

                // Water Glass Cup Animation Wave simulation
                WaterWaveAnimation()
            }
        }
    }
}

@Composable
fun getBmiColor(category: String): Color {
    return when (category) {
        "Underweight" -> Color.Blue
        "Normal" -> Color(0xFF4CAF50)
        "Overweight" -> Color(0xFFFF9800)
        "Obese" -> Color.Red
        else -> Color.Gray
    }
}

@Composable
fun MacroProgressLine(name: String, grams: Int, ratio: Float, tint: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("${grams}g (${(grams * (if (name == "Fats") 9 else 4))} kcal)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = ratio,
            color = tint,
            trackColor = Color.LightGray.copy(alpha = 0.3f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun WaterWaveAnimation() {
    val transition = rememberInfiniteTransition()
    val waveOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .size(60.dp, 80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(2.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .graphicsLayer {
                    translationY = sin(waveOffset) * 4
                }
                .background(Color(0xFF2196F3))
        )
        Icon(
            imageVector = Icons.Default.WaterDrop,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Center)
        )
    }
}

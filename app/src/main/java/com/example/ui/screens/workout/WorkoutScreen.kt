package com.example.ui.screens.workout

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.entities.ExerciseLog
import com.example.ui.screens.meals.UiState
import com.example.utils.formatDateToHumanString
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel = viewModel()
) {
    val exercises by viewModel.exercises.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState()
    val estimationState by viewModel.estimationState.collectAsState()

    val stepState by viewModel.stepLog.collectAsState()
    val steps = stepState?.steps ?: 0
    val targetSteps = stepState?.targetSteps ?: 10000
    val stepCalories = stepState?.caloriesBurned ?: 0.0

    var showAddDialog by remember { mutableStateOf(false) }

    var exerciseType by remember { mutableStateOf("Cardio") } // Cardio, Strength, Yoga, Sports, Other
    var exerciseNameCustom by remember { mutableStateOf("") }
    var durationStr by remember { mutableStateOf("30") }
    var intensity by remember { mutableStateOf("Medium") } // Low, Medium, High
    var customCaloriesStr by remember { mutableStateOf("") }
    var useManualCalories by remember { mutableStateOf(false) }

    val totalBurnedToday = exercises.sumOf { it.caloriesBurned } + stepCalories

    val context = LocalContext.current
    var isSensorListening by remember { mutableStateOf(false) }

    val permissionRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isSensorListening = true
        } else {
            Toast.makeText(context, "Sensor activity recognition permission is required for pedometer tracking.", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(isSensorListening) {
        if (!isSensorListening) return@DisposableEffect onDispose {}

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        var initialStepCount = -1f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                    val measuredSteps = event.values[0]
                    if (initialStepCount < 0f) {
                        initialStepCount = measuredSteps
                    }
                    val newDiff = (measuredSteps - initialStepCount).toInt()
                    if (newDiff > 0) {
                        viewModel.updateSteps(steps + newDiff, targetSteps)
                        initialStepCount = measuredSteps
                    }
                } else if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                    val detected = event.values[0].toInt()
                    if (detected > 0) {
                        viewModel.updateSteps(steps + detected, targetSteps)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        var registered = false
        if (stepDetectorSensor != null) {
            registered = sensorManager.registerListener(listener, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else if (stepCounterSensor != null) {
            registered = sensorManager.registerListener(listener, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
        }

        if (!registered) {
            Toast.makeText(context, "No hardware pedometer sensor detected. Using Simulation tools.", Toast.LENGTH_SHORT).show()
            isSensorListening = false
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout & Exercise", fontWeight = FontWeight.Black) },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add workout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add workout")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dashboard Today's Burn Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "BURNED CALORIES TODAY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${totalBurnedToday.roundToInt()} kcal",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }

            // Pedometer Progress Card
            item {
                PedometerCard(
                    steps = steps,
                    targetSteps = targetSteps,
                    burnedKcal = stepCalories,
                    isListening = isSensorListening,
                    onToggleListener = { enable ->
                        if (enable) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val permissionCheck = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACTIVITY_RECOGNITION
                                )
                                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                    isSensorListening = true
                                } else {
                                    permissionRequestLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                }
                            } else {
                                isSensorListening = true
                            }
                        } else {
                            isSensorListening = false
                        }
                    },
                    onSimulateSteps = { diff ->
                        val newSteps = (steps + diff).coerceAtLeast(0)
                        viewModel.updateSteps(newSteps, targetSteps)
                    },
                    onUpdateTarget = { newTarget ->
                        viewModel.updateSteps(steps, newTarget)
                    }
                )
            }

            // Today's logs category
            item {
                Text(
                    "TODAY'S WORKOUTS",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (exercises.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No workouts logged today. Let's move! 🏃‍♀️", fontSize = 13.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            } else {
                items(exercises) { log ->
                    ExerciseRowItem(log = log, onDelete = { viewModel.deleteExercise(it) })
                }
            }

            // History Log section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "WORKOUT HISTORY",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            val historicItems = allExercises.filter { it.date != viewModel.selectedDate.value }
            if (historicItems.isEmpty()) {
                item {
                    Text(
                        "No past history available yet.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                items(historicItems) { log ->
                    ExerciseRowItem(log = log, showDate = true, onDelete = { viewModel.deleteExercise(it) })
                }
            }
        }

        // Add Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAddDialog = false
                    viewModel.clearEstimationState()
                },
                title = { Text("Log New Workout", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Exercise Category:")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val categories = listOf("Cardio", "Strength", "Yoga", "Sports", "Other")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(categories) { cat ->
                                    val sel = exerciseType == cat
                                    FilterChip(
                                        selected = sel,
                                        onClick = { exerciseType = cat },
                                        label = { Text(cat, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = exerciseNameCustom,
                            onValueChange = { exerciseNameCustom = it },
                            label = { Text("Workout Name / Description") },
                            placeholder = { Text("e.g., Treadmill, swimming, squats") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = durationStr,
                                onValueChange = { durationStr = it },
                                label = { Text("Duration (min)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Intensity", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    listOf("Low", "Med", "High").forEach { choice ->
                                        val fullChoice = when (choice) {
                                            "Low" -> "Low"
                                            "Med" -> "Medium"
                                            else -> "High"
                                        }
                                        val isSelected = intensity == fullChoice
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f))
                                                .clickable { intensity = fullChoice }
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(choice, color = if (isSelected) Color.White else Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = useManualCalories, onCheckedChange = { useManualCalories = it })
                            Text("Enter Calories Manually (Optional)", fontSize = 13.sp)
                        }

                        if (useManualCalories) {
                            OutlinedTextField(
                                value = customCaloriesStr,
                                onValueChange = { customCaloriesStr = it },
                                label = { Text("Calories Burned (kcal)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (estimationState is UiState.Loading) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val duration = durationStr.toIntOrNull() ?: 30
                            val typeName = exerciseNameCustom.ifBlank { exerciseType }
                            val manualCal = if (useManualCalories) customCaloriesStr.toDoubleOrNull() else null
                            viewModel.addExercise(typeName, duration, intensity, manualCal)
                            showAddDialog = false
                            exerciseNameCustom = ""
                            customCaloriesStr = ""
                            useManualCalories = false
                        },
                        enabled = durationStr.isNotBlank() && (exerciseNameCustom.isNotBlank() || exerciseType.isNotBlank())
                    ) {
                        Text("Add Workout")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ExerciseRowItem(
    log: ExerciseLog,
    showDate: Boolean = false,
    onDelete: (ExerciseLog) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (log.exerciseType.lowercase()) {
                            "yoga" -> Icons.Default.SelfImprovement
                            "strength" -> Icons.Default.FitnessCenter
                            "sports" -> Icons.Default.SportsBasketball
                            else -> Icons.Default.DirectionsRun
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(log.exerciseType, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "${log.durationMin} min • Intensity: ${log.intensity}" + if (showDate) " • ${log.createdAt.formatDateToHumanString()}" else "",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "-${log.caloriesBurned.roundToInt()} kcal",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { onDelete(log) }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun PedometerCard(
    steps: Int,
    targetSteps: Int,
    burnedKcal: Double,
    isListening: Boolean,
    onToggleListener: (Boolean) -> Unit,
    onSimulateSteps: (Int) -> Unit,
    onUpdateTarget: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var inputGoalStr by remember { mutableStateOf(targetSteps.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = "Pedometer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DAILY STEPS TRACKER",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Autotrack Toggle Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isListening) "Track ON" else "Track OFF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isListening) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Switch(
                        checked = isListening,
                        onCheckedChange = onToggleListener,
                        thumbContent = if (isListening) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        } else null,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.scale(0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main stats layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Steps circular/box layout
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val percentage = (steps.toFloat() / targetSteps.toFloat()).coerceIn(0f, 1f)
                    val animatedPercentage by animateFloatAsState(
                        targetValue = percentage,
                        label = "Step gauge animation"
                    )

                    // Draw circular loader
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            startAngle = -180f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = if (percentage >= 1f) Color(0xFF4CAF50) else Color(0xFF2196F3),
                            startAngle = -90f,
                            sweepAngle = animatedPercentage * 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%,d", steps),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "steps",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Stats and configuration
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Goal: " + String.format("%,d", targetSteps),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Burned: " + String.format("%.1f", burnedKcal) + " kcal",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { showDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            "Change Goal",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Simulation Section for Emulator/Testing
            Text(
                text = "DEMO & SIMULATION TOOLS",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onSimulateSteps(1000) },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Icon(Icons.Default.DirectionsRun, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Walk +1k", fontSize = 10.sp)
                }

                OutlinedButton(
                    onClick = { onSimulateSteps(5000) },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Jog +5k", fontSize = 10.sp)
                }

                OutlinedButton(
                    onClick = { onSimulateSteps(-steps) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = BorderStroke(1.dp, Color.Red),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(0.8f)
                        .height(34.dp)
                ) {
                    Text("Reset", fontSize = 10.sp)
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Update Steps Goal", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = inputGoalStr,
                    onValueChange = { inputGoalStr = it },
                    label = { Text("Target Daily Steps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val input = inputGoalStr.toIntOrNull() ?: 10000
                        onUpdateTarget(input)
                        showDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

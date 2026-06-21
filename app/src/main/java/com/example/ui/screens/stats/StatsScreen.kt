package com.example.ui.screens.stats

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.entities.WeightLog
import com.example.data.entities.FoodLog
import com.example.utils.formatDateToHumanString
import com.example.utils.formatOneDecimal
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = viewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val weightHistory by viewModel.weightHistory.collectAsState()
    val weeklyFoodLogs by viewModel.weeklyFoodLogsState.collectAsState()
    val streakCount by viewModel.streakCount.collectAsState()

    var showWeightDialog by remember { mutableStateOf(false) }
    var weightInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    val currentWeight = userProfile?.weightKg ?: 70.0
    val targetWeight = userProfile?.targetWeightKg ?: 65.0
    val weightDiff = (currentWeight - targetWeight)
    val weightDiffLabel = if (weightDiff > 0) "to lose" else "to gain"
    val progressPct = ((currentWeight / targetWeight.coerceAtLeast(1.0)) * 100).coerceAtMost(100.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress & Analytics", fontWeight = FontWeight.Black) },
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
            // Streak Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "🔥 $streakCount-Day Logging Streak!",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Consistency is the key to healthy metabolism. Keep it up!",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // Weight Log Core Header Map
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Weight", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("${currentWeight.formatOneDecimal()} kg", fontSize = 24.sp, fontWeight = FontWeight.Black)
                        }
                        
                        TextButton(
                            onClick = {
                                weightInput = currentWeight.toString()
                                showWeightDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Weight")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Goal Weight: ${targetWeight} kg (${kotlin.math.abs(weightDiff).formatOneDecimal()} kg $weightDiffLabel)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Draw Weight Trend Line Chart
            if (weightHistory.size >= 2) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("30-DAY WEIGHT TREND", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        WeightTrendLineChart(weightHistory)
                    }
                }
            }

            // Draw Weekly Calories Bar Chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("WEEKLY CALORIES VS BUDGET", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    WeeklyCalorieBarChart(weeklyFoodLogs, userProfile?.targetCalories ?: 2000)
                }
            }

            // Draw Weekly Macro averages
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("WEEKLY AVERAGE MACROS BALANCE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    WeeklyMacrosPieChart(weeklyFoodLogs)
                }
            }

            // Weekly summary text values
            WeeklySummaryBannerCard(weeklyFoodLogs, userProfile?.targetCalories ?: 2000)
        }

        // Weight Input Alert Dialog
        if (showWeightDialog) {
            AlertDialog(
                onDismissRequest = { showWeightDialog = false },
                title = { Text("Log Body Weight") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("Notes (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val w = weightInput.toDoubleOrNull()
                            if (w != null) {
                                viewModel.logWeight(w, notesInput)
                                showWeightDialog = false
                            }
                        }
                    ) {
                        Text("Log Weight")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWeightDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun WeightTrendLineChart(history: List<WeightLog>) {
    val points = history.takeLast(10) // show last 10 records
    val minWeight = (points.minOfOrNull { it.weightKg } ?: 50.0) - 2
    val maxWeight = (points.maxOfOrNull { it.weightKg } ?: 80.0) + 2
    val weightRange = maxWeight - minWeight

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(horizontal = 16.dp)
    ) {
        val width = size.width
        val height = size.height

        val spaceX = width / (points.size - 1).coerceAtLeast(1)
        val path = Path()

        points.forEachIndexed { index, weightLog ->
            val x = index * spaceX
            // normalise Y between height limits
            val ratioY = ((weightLog.weightKg - minWeight) / weightRange).toFloat()
            val y = height - (ratioY * height)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = Color(0xFF2196F3),
            style = Stroke(width = 4.dp.toPx())
        )

        // Draw node circles
        points.forEachIndexed { index, weightLog ->
            val x = index * spaceX
            val ratioY = ((weightLog.weightKg - minWeight) / weightRange).toFloat()
            val y = height - (ratioY * height)
            drawCircle(
                color = Color(0xFF0D47A1),
                radius = 6.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun WeeklyCalorieBarChart(logs: List<FoodLog>, dailyGoal: Int) {
    val group = logs.groupBy { it.date }
    // Last 7 days dates helper
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val cal = java.util.Calendar.getInstance()
    
    val past7Days = mutableListOf<String>()
    for (i in 0..6) {
        past7Days.add(sdf.format(cal.time))
        cal.add(java.util.Calendar.DATE, -1)
    }
    past7Days.reverse() // Chronological order

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(horizontal = 8.dp)
    ) {
        val width = size.width
        val height = size.height

        val barSpaceX = width / 7
        val barWidth = barSpaceX * 0.6f

        past7Days.forEachIndexed { index, dateStr ->
            val eatenCal = group[dateStr]?.sumOf { it.calories } ?: 0.0
            val ratio = (eatenCal / dailyGoal).coerceAtMost(1.5).toFloat()
            
            val barHeight = height * ratio * 0.65f // Scale factor to fit inside canvas height
            
            val barX = (index * barSpaceX) + (barSpaceX - barWidth) / 2
            val barY = height - barHeight

            val barColor = if (eatenCal > dailyGoal) Color.Red else Color(0xFF4CAF50)

            drawRect(
                color = barColor,
                topLeft = Offset(barX, barY),
                size = Size(barWidth, barHeight)
            )
        }

        // Draw daily budget boundary line
        val lineY = height - (height * 0.65f)
        drawLine(
            color = Color.Gray,
            start = Offset(0f, lineY),
            end = Offset(width, lineY),
            strokeWidth = 2f
        )
    }
}

@Composable
fun WeeklyMacrosPieChart(logs: List<FoodLog>) {
    val totalProtein = logs.sumOf { it.proteinG }
    val totalCarbs = logs.sumOf { it.carbsG }
    val totalFats = logs.sumOf { it.fatG }
    val macroSum = (totalProtein + totalCarbs + totalFats).coerceAtLeast(1.0)

    val proteinPct = (totalProtein / macroSum).toFloat()
    val carbsPct = (totalCarbs / macroSum).toFloat()
    val fatsPct = (totalFats / macroSum).toFloat()

    Canvas(
        modifier = Modifier
            .size(140.dp)
            .padding(16.dp)
    ) {
        val width = size.width
        val height = size.height
        val radius = width.coerceAtMost(height)

        val activeProtAngle = proteinPct * 360f
        val activeCarbAngle = carbsPct * 360f
        val activeFatAngle = fatsPct * 360f

        // Draw Protein Arc
        drawArc(
            color = Color(0xFFFF5722),
            startAngle = 0f,
            sweepAngle = activeProtAngle,
            useCenter = true,
            size = Size(radius, radius)
        )

        // Draw Carbs Arc
        drawArc(
            color = Color(0xFF4CAF50),
            startAngle = activeProtAngle,
            sweepAngle = activeCarbAngle,
            useCenter = true,
            size = Size(radius, radius)
        )

        // Draw Fats Arc
        drawArc(
            color = Color(0xFFFFEB3B),
            startAngle = activeProtAngle + activeCarbAngle,
            sweepAngle = activeFatAngle,
            useCenter = true,
            size = Size(radius, radius)
        )
    }
}

@Composable
fun WeeklySummaryBannerCard(logs: List<FoodLog>, dailyGoal: Int) {
    val group = logs.groupBy { it.date }
    val dailyTotals = group.values.map { dayList -> dayList.sumOf { it.calories } }

    val averageCalories = if (dailyTotals.isNotEmpty()) dailyTotals.average() else 0.0
    val maxDay = if (dailyTotals.isNotEmpty()) dailyTotals.maxOrNull() ?: 0.0 else 0.0
    val minDay = if (dailyTotals.isNotEmpty()) dailyTotals.minOrNull() ?: 0.0 else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("WEEKLY HIGHLIGHTS SUMMARY", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Average Daily Intake:", fontSize = 13.sp)
                Text("${averageCalories.roundToInt()} kcal / day", fontWeight = FontWeight.Bold)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Highest Consumed Day:", fontSize = 13.sp)
                Text("${maxDay.roundToInt()} kcal", fontWeight = FontWeight.Bold, color = Color.Red)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Lowest Consumed Day:", fontSize = 13.sp)
                Text("${minDay.roundToInt()} kcal", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            }
        }
    }
}

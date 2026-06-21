package com.example.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.entities.FrequentFood
import com.example.utils.getGreetingMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    onNavigateToTab: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val userProfile by viewModel.userProfile.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    
    val eatenCal by viewModel.eatenCalories.collectAsState()
    val eatenProt by viewModel.eatenProtein.collectAsState()
    val eatenCarbs by viewModel.eatenCarbs.collectAsState()
    val eatenFats by viewModel.eatenFats.collectAsState()
    val burnedCal by viewModel.burnedCalories.collectAsState()

    val waterLog by viewModel.waterLog.collectAsState()
    val quickAddItems by viewModel.quickAddFoods.collectAsState()

    val targetCal = userProfile?.targetCalories ?: 2000
    val targetProt = userProfile?.targetProtein ?: 120
    val targetCarb = userProfile?.targetCarbs ?: 220
    val targetFat = userProfile?.targetFat ?: 65
    val waterTargetGlass = (userProfile?.waterTargetMl ?: 2000.0) / 250.0

    val netCalories = eatenCal - burnedCal
    val caloriesRemaining = (targetCal - netCalories).roundToInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = getGreetingMessage(userProfile?.name ?: "Achiever"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Quick Calendar Indicator
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, size = 16.dp)
                    Text("Today", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Circular Calorie Budget Progress Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CALORIE REMAINING BUDGET",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Gauge ring
                    val progress = (netCalories / targetCal.toDouble()).coerceIn(0.0, 1.0).toFloat()
                    val ringColor = when {
                        caloriesRemaining < 0 -> Color.Red
                        caloriesRemaining < 200 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    }

                    // Animate angle
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = tween(1200, easing = FastOutSlowInEasing)
                    )

                    androidx.compose.foundation.Canvas(modifier = Modifier.size(190.dp)) {
                        // Background track
                        drawArc(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            startAngle = -220f,
                            sweepAngle = 260f,
                            useCenter = false,
                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Active track
                        drawArc(
                            color = ringColor,
                            startAngle = -220f,
                            sweepAngle = animatedProgress * 260f,
                            useCenter = false,
                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (caloriesRemaining >= 0) "$caloriesRemaining" else "${-caloriesRemaining}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = ringColor
                        )
                        Text(
                            text = if (caloriesRemaining >= 0) "kcal Left" else "kcal Over",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats footer: Burned, Eaten, Goal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CalorieStatMini(title = "Eaten", value = eatenCal.toInt(), color = Color(0xFFA1887F))
                    CalorieStatMini(title = "Burned", value = burnedCal.toInt(), color = Color(0xFF2196F3))
                    CalorieStatMini(title = "Daily Goal", value = targetCal, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Macro Progress Bars
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "DAILY MACRO BALANCE",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                MacroSlideBar(
                    name = "Protein",
                    current = eatenProt,
                    target = targetProt.toDouble(),
                    tint = Color(0xFFFF5722),
                    unit = "g"
                )

                MacroSlideBar(
                    name = "Carbs",
                    current = eatenCarbs,
                    target = targetCarb.toDouble(),
                    tint = Color(0xFF4CAF50),
                    unit = "g"
                )

                MacroSlideBar(
                    name = "Fats",
                    current = eatenFats,
                    target = targetFat.toDouble(),
                    tint = Color(0xFFFFEB3B),
                    unit = "g"
                )
            }
        }

        // Fasting Timer & Cheat Mode Widgets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Fasting widget
            val isFastingActive by viewModel.isFastingActive.collectAsState()
            
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.toggleFasting() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFastingActive) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = if (isFastingActive) Color(0xFF2E7D32) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Fast Timer", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (isFastingActive) "ACTIVE" else "Start",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isFastingActive) Color(0xFF2E7D32) else Color.Gray
                    )
                }
            }

            // Quick Camera Scan Action Shortcut
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToTab("Meals") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Camera Scan", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("AI log meals", fontSize = 10.sp, color = Color.Gray)
                }
            }

            // Quick Goal Action Shortcut
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToTab("Meals") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Add Food", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("Manual entry", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }

        // Water Tracker Card
        val glassesDrunk = waterLog?.glassesCount ?: 0
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "WATER TRACKER",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "$glassesDrunk of ${waterTargetGlass.roundToInt()} glasses drunk today",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.decrementWater() },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFE3F2FD))
                        ) {
                            Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1976D2))
                        }
                        IconButton(
                            onClick = { viewModel.incrementWater() },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom grid of glass cups
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items((1..waterTargetGlass.roundToInt().coerceAtLeast(8)).toList()) { index ->
                        val filled = index <= glassesDrunk
                        WaterGlassIcon(filled = filled, onClick = {
                            if (filled) {
                                viewModel.decrementWater()
                            } else {
                                viewModel.incrementWater()
                            }
                        })
                    }
                }
            }
        }

        // Quick Add horizontals scroll
        if (quickAddItems.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "RE-ADD RECENT FAVORITES",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickAddItems) { food ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.addQuickFood(food) },
                            label = { Text("${food.foodName} (+${food.avgCalories.roundToInt()} kcal)") },
                            leadingIcon = { Icon(Icons.Default.PlusOne, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalorieStatMini(title: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = title,
            fontSize = 11.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MacroSlideBar(name: String, current: Double, target: Double, tint: Color, unit: String) {
    val progressFraction = if (target > 0) (current / target).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, animationSpec = tween(800))

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
                text = "${current.roundToInt()}$unit / ${target.roundToInt()}$unit",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = animatedProgress,
            color = tint,
            trackColor = Color.LightGray.copy(alpha = 0.3f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

@Composable
fun WaterGlassIcon(filled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp, 48.dp)
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
            .background(if (filled) Color(0xFF2196F3) else Color.LightGray.copy(alpha = 0.3f))
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (filled) Icons.Default.WaterDrop else Icons.Outlined.WaterDrop,
            contentDescription = null,
            tint = if (filled) Color.White else Color(0xFF2196F3),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = Modifier.size(size)
    )
}

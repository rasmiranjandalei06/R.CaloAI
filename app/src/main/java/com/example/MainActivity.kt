package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.coaching.CoachingScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.meals.MealsScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.stats.StatsScreen
import com.example.ui.screens.workout.WorkoutScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val repository = (context.applicationContext as NutriTrackApp).repository
            
            val userProfileState = repository.userProfileFlow.collectAsState(initial = null)
            val userProfile = userProfileState.value

            val darkTheme = userProfile?.darkMode ?: isSystemInDarkTheme()

            MyApplicationTheme(darkTheme = darkTheme) {
                if (userProfile == null) {
                    // Loading overlay while determining database status
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        OnboardingScreen(onOnboardingComplete = {
                            // Automatically triggers state updates once database writes complete
                        })
                    }
                } else {
                    // Main layout
                    NutriTrackHomeNavGraph()
                }
            }
        }
    }
}

@Composable
fun NutriTrackHomeNavGraph() {
    var activeTab by remember { mutableStateOf("Home") } // Home, Meals, Workout, Stats, Profile, Coaching
    var previousTabBeforeCoach by remember { mutableStateOf("Home") }

    Scaffold(
        bottomBar = {
            if (activeTab != "Coaching") {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val tabs = listOf(
                        Triple("Home", Icons.Default.Home, "Summary"),
                        Triple("Meals", Icons.Default.Restaurant, "Meals"),
                        Triple("Workout", Icons.Default.FitnessCenter, "Workouts"),
                        Triple("Stats", Icons.Default.BarChart, "Progress"),
                        Triple("Profile", Icons.Default.Settings, "Profile")
                    )

                    tabs.forEach { (route, icon, label) ->
                        val isSelected = activeTab == route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { activeTab = route },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (activeTab != "Coaching") {
                FloatingActionButton(
                    onClick = {
                        previousTabBeforeCoach = activeTab
                        activeTab = "Coaching"
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "AI Coach Support")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "Home" -> HomeScreen(onNavigateToTab = { activeTab = it })
                "Meals" -> MealsScreen()
                "Workout" -> WorkoutScreen()
                "Stats" -> StatsScreen()
                "Profile" -> ProfileScreen(onNavigateToOnboarding = {})
                "Coaching" -> CoachingScreen(onBack = { activeTab = previousTabBeforeCoach })
            }
        }
    }
}

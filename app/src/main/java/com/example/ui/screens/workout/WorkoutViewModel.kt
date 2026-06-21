package com.example.ui.screens.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NutriTrackApp
import com.example.data.entities.ExerciseLog
import com.example.data.entities.UserProfile
import com.example.data.entities.StepLog
import com.example.data.repository.NutritionRepository
import com.example.gemini.GeminiService
import com.example.ui.screens.meals.UiState
import com.example.utils.getCurrentDateString
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NutritionRepository = (application as NutriTrackApp).repository

    val userProfile: StateFlow<UserProfile?> = repository.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedDate = MutableStateFlow(getCurrentDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Exercises logged for selected date
    val exercises: StateFlow<List<ExerciseLog>> = _selectedDate
        .flatMapLatest { date -> repository.getExercisesByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Steps logged for selected date
    val stepLog: StateFlow<StepLog?> = _selectedDate
        .flatMapLatest { date -> repository.getStepsByDateFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // All historic exercises
    val allExercises: StateFlow<List<ExerciseLog>> = repository.allExercisesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _estimationState = MutableStateFlow<UiState<Double>>(UiState.Idle)
    val estimationState: StateFlow<UiState<Double>> = _estimationState.asStateFlow()

    fun clearEstimationState() {
        _estimationState.value = UiState.Idle
    }

    fun updateSteps(steps: Int, target: Int = 10000) {
        viewModelScope.launch {
            repository.updateSteps(_selectedDate.value, steps, target)
        }
    }

    fun addExercise(type: String, duration: Int, intensity: String, customCalories: Double? = null) {
        viewModelScope.launch {
            _estimationState.value = UiState.Loading
            
            val userWeight = userProfile.value?.weightKg ?: 70.0
            val key = userProfile.value?.geminiApiKey ?: ""

            val calories = if (customCalories != null) {
                customCalories
            } else {
                // Query Gemini for calorie estimations
                val estimatedVal = GeminiService.estimateWorkoutCalories(type, duration, intensity, userWeight, key)
                // Fallback estimation calculation in case API has issue
                estimatedVal ?: estimateBurnedCaloriesFallback(type, duration, intensity, userWeight)
            }

            val log = ExerciseLog(
                date = _selectedDate.value,
                exerciseType = type,
                durationMin = duration,
                intensity = intensity,
                caloriesBurned = calories
            )

            repository.insertExercise(log)
            _estimationState.value = UiState.Success(calories)
        }
    }

    fun deleteExercise(exerciseLog: ExerciseLog) {
        viewModelScope.launch {
            repository.deleteExercise(exerciseLog)
        }
    }

    // High performance fallback calculations based on Metabolic Equivalent of Task (MET)
    private fun estimateBurnedCaloriesFallback(type: String, duration: Int, intensity: String, weightKg: Double): Double {
        val baseMet = when (type.lowercase()) {
            "cardio" -> if (intensity == "High") 10.0 else if (intensity == "Medium") 7.0 else 4.0
            "strength" -> if (intensity == "High") 6.0 else if (intensity == "Medium") 4.5 else 3.0
            "yoga" -> if (intensity == "High") 4.0 else if (intensity == "Medium") 3.0 else 2.0
            "sports" -> if (intensity == "High") 9.0 else if (intensity == "Medium") 7.0 else 5.5
            else -> 4.5 // Default/Other
        }
        // Formula: Calories burned = MET * weight (kg) * (duration / 60)
        return baseMet * weightKg * (duration / 60.0)
    }
}

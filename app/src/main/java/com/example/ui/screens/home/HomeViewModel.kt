package com.example.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NutriTrackApp
import com.example.data.entities.FoodLog
import com.example.data.entities.FrequentFood
import com.example.data.entities.UserProfile
import com.example.data.entities.WaterLog
import com.example.data.entities.StepLog
import com.example.data.repository.NutritionRepository
import com.example.utils.getCurrentDateString
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NutritionRepository = (application as NutriTrackApp).repository

    val userProfile: StateFlow<UserProfile?> = repository.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedDate = MutableStateFlow(getCurrentDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Foods logged for selected date
    val foodLogs: StateFlow<List<FoodLog>> = _selectedDate
        .flatMapLatest { date -> repository.getFoodsByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Water logs for selected date
    val waterLog: StateFlow<WaterLog?> = _selectedDate
        .flatMapLatest { date -> repository.getWaterByDateFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Exercises logged for selected date
    val exercises: StateFlow<List<com.example.data.entities.ExerciseLog>> = _selectedDate
        .flatMapLatest { date -> repository.getExercisesByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Step loads for selected date
    val stepLog: StateFlow<StepLog?> = _selectedDate
        .flatMapLatest { date -> repository.getStepsByDateFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Frequent / Recent foods for quick add chips
    val quickAddFoods: StateFlow<List<FrequentFood>> = repository.frequentFoodsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Macro aggregates
    val eatenCalories = foodLogs.map { list -> list.sumOf { it.calories } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val eatenProtein = foodLogs.map { list -> list.sumOf { it.proteinG } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val eatenCarbs = foodLogs.map { list -> list.sumOf { it.carbsG } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val eatenFats = foodLogs.map { list -> list.sumOf { it.fatG } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val burnedCalories = combine(exercises, stepLog) { exList, steps ->
        val exBurned = exList.sumOf { it.caloriesBurned }
        val stepsBurned = steps?.caloriesBurned ?: 0.0
        exBurned + stepsBurned
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun changeDate(date: String) {
        _selectedDate.value = date
    }

    fun addQuickFood(food: FrequentFood) {
        viewModelScope.launch {
            val newLog = FoodLog(
                date = _selectedDate.value,
                mealType = "SNACKS",
                foodName = food.foodName,
                quantity = "1 portion",
                calories = food.avgCalories,
                proteinG = food.avgProtein,
                carbsG = (food.avgCalories * 0.4 / 4).coerceAtLeast(0.0), // Approximate estimation
                fatG = (food.avgCalories * 0.3 / 9).coerceAtLeast(0.0)
            )
            repository.insertFood(newLog)
        }
    }

    fun incrementWater() {
        viewModelScope.launch {
            repository.incrementWater(_selectedDate.value)
        }
    }

    fun decrementWater() {
        viewModelScope.launch {
            repository.decrementWater(_selectedDate.value)
        }
    }

    // Intermittent Fasting State management
    val isFastingActive: StateFlow<Boolean> = userProfile.map { it?.fastingMode ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val fastingTimeElapsed: StateFlow<Long> = userProfile.map { it?.fastingStartTime ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun toggleFasting() {
        viewModelScope.launch {
            val currentProfile = userProfile.value
            if (currentProfile != null) {
                val newFastingState = !currentProfile.fastingMode
                val newStartTime = if (newFastingState) System.currentTimeMillis() else 0L
                val updatedProfile = currentProfile.copy(
                    fastingMode = newFastingState,
                    fastingStartTime = newStartTime
                )
                repository.saveUserProfile(updatedProfile)
            }
        }
    }
}

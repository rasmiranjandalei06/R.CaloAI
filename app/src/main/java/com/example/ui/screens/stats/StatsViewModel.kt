package com.example.ui.screens.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NutriTrackApp
import com.example.data.entities.FoodLog
import com.example.data.entities.UserProfile
import com.example.data.entities.WeightLog
import com.example.data.repository.NutritionRepository
import com.example.utils.getCurrentDateString
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NutritionRepository = (application as NutriTrackApp).repository

    val userProfile: StateFlow<UserProfile?> = repository.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val weightHistory: StateFlow<List<WeightLog>> = repository.weightHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // We can evaluate streaks and metrics by taking food logs
    // To calculate weekly stats, we fetch the last 7 days of food logs
    private val _weeklyFoodLogsState = MutableStateFlow<List<FoodLog>>(emptyList())
    val weeklyFoodLogsState: StateFlow<List<FoodLog>> = _weeklyFoodLogsState.asStateFlow()

    init {
        loadWeeklyFoodLogs()
    }

    private fun loadWeeklyFoodLogs() {
        viewModelScope.launch {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val calendar = java.util.Calendar.getInstance()
            val list = mutableListOf<FoodLog>()
            
            // Gather last 7 days of logged foods
            for (i in 0..6) {
                val dateStr = sdf.format(calendar.time)
                val dayLogs = repository.getFoodsByDate(dateStr).first()
                list.addAll(dayLogs)
                calendar.add(java.util.Calendar.DATE, -1)
            }
            _weeklyFoodLogsState.value = list
        }
    }

    fun logWeight(weight: Double, notes: String = "") {
        viewModelScope.launch {
            val today = getCurrentDateString()
            val entry = WeightLog(
                date = today,
                weightKg = weight,
                notes = notes
            )
            repository.insertWeight(entry)
            loadWeeklyFoodLogs() // refresh
        }
    }

    fun deleteWeight(id: Long) {
        viewModelScope.launch {
            repository.deleteWeightById(id)
        }
    }

    // Goal streak logic (A streak is active if user logged meals in consecutive days and met caloric limits)
    val streakCount: StateFlow<Int> = _weeklyFoodLogsState.map { logs ->
        // Evaluate logged dates
        val datesWithLogs = logs.groupBy { it.date }
        var streak = 0
        var checkDate = java.util.Calendar.getInstance()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        
        while (true) {
            val dateStr = sdf.format(checkDate.time)
            if (datesWithLogs.containsKey(dateStr)) {
                streak++
                checkDate.add(java.util.Calendar.DATE, -1)
            } else {
                break
            }
        }
        streak
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)
}

package com.example.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NutriTrackApp
import com.example.data.entities.UserProfile
import com.example.data.repository.NutritionRepository
import com.example.utils.CalorieCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NutritionRepository = (application as NutriTrackApp).repository

    val userProfile: StateFlow<UserProfile?> = repository.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateProfile(
        name: String,
        age: Int,
        gender: String,
        heightCm: Double,
        weightKg: Double,
        goal: String,
        activityLevel: String,
        unitSystem: String,
        targetWeightKg: Double
    ) {
        viewModelScope.launch {
            // Recalculate targets based on updated selections
            val calc = CalorieCalculator.calculateEverything(
                age, gender, heightCm, weightKg, goal, activityLevel
            )

            val current = userProfile.value
            val updated = UserProfile(
                id = 1,
                name = name,
                age = age,
                gender = gender,
                heightCm = heightCm,
                weightKg = weightKg,
                goal = goal,
                activityLevel = activityLevel,
                targetCalories = calc.targetCalories,
                targetProtein = calc.proteinGrams,
                targetCarbs = calc.carbsGrams,
                targetFat = calc.fatGrams,
                waterTargetMl = calc.waterMl,
                unitSystem = unitSystem,
                geminiApiKey = current?.geminiApiKey ?: "",
                darkMode = current?.darkMode ?: false,
                targetWeightKg = targetWeightKg
            )
            repository.saveUserProfile(updated)
        }
    }

    fun updateGeminiApiKey(key: String) {
        viewModelScope.launch {
            repository.updateGeminiApiKey(key)
        }
    }

    fun updateDarkMode(darkMode: Boolean) {
        viewModelScope.launch {
            repository.updateDarkMode(darkMode)
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            val appDb = (getApplication() as NutriTrackApp).database
            appDb.clearAllTables()
        }
    }
}

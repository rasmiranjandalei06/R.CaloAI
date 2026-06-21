package com.example.ui.screens.meals

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NutriTrackApp
import com.example.data.entities.FoodLog
import com.example.data.entities.UserProfile
import com.example.data.repository.NutritionRepository
import com.example.gemini.GeminiService
import com.example.gemini.FoodAnalysisResponse
import com.example.gemini.CameraScanResponse
import com.example.gemini.RecipeAnalysisResponse
import com.example.utils.getCurrentDateString
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class MealsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NutritionRepository = (application as NutriTrackApp).repository

    val userProfile: StateFlow<UserProfile?> = repository.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedDate = MutableStateFlow(getCurrentDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Foods logged for selected date
    val foodLogs: StateFlow<List<FoodLog>> = _selectedDate
        .flatMapLatest { date -> repository.getFoodsByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI states for asynchronous operations
    private val _searchState = MutableStateFlow<UiState<FoodAnalysisResponse>>(UiState.Idle)
    val searchState: StateFlow<UiState<FoodAnalysisResponse>> = _searchState.asStateFlow()

    private val _cameraScanState = MutableStateFlow<UiState<CameraScanResponse>>(UiState.Idle)
    val cameraScanState: StateFlow<UiState<CameraScanResponse>> = _cameraScanState.asStateFlow()

    private val _recipeState = MutableStateFlow<UiState<RecipeAnalysisResponse>>(UiState.Idle)
    val recipeState: StateFlow<UiState<RecipeAnalysisResponse>> = _recipeState.asStateFlow()

    val favoriteFoods = repository.favoriteFoodsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearSearchState() {
        _searchState.value = UiState.Idle
    }

    fun clearCameraScanState() {
        _cameraScanState.value = UiState.Idle
    }

    fun clearRecipeState() {
        _recipeState.value = UiState.Idle
    }

    // Direct insert of manual food item or parsed item
    fun addFoodLog(mealType: String, name: String, qty: String, cals: Double, prot: Double, carbs: Double, fat: Double, favorited: Boolean = false) {
        viewModelScope.launch {
            val log = FoodLog(
                date = _selectedDate.value,
                mealType = mealType,
                foodName = name,
                quantity = qty,
                calories = cals,
                proteinG = prot,
                carbsG = carbs,
                fatG = fat,
                isFavorite = favorited
            )
            repository.insertFood(log)
        }
    }

    fun deleteFood(foodLog: FoodLog) {
        viewModelScope.launch {
            repository.deleteFood(foodLog)
        }
    }

    fun toggleFavoriteFood(foodLog: FoodLog) {
        viewModelScope.launch {
            repository.insertFood(foodLog.copy(isFavorite = !foodLog.isFavorite))
        }
    }

    // AI Text Analyzer
    fun searchFoodItemWithAI(query: String) {
        viewModelScope.launch {
            _searchState.value = UiState.Loading
            val key = userProfile.value?.geminiApiKey ?: ""
            val result = GeminiService.analyzeTextFood(query, key)
            if (result != null) {
                _searchState.value = UiState.Success(result)
            } else {
                _searchState.value = UiState.Error("Failed to extract nutrition metrics from Gemini. Please verify your query or your API key.")
            }
        }
    }

    // AI Camera Vision Scanner
    fun scanCameraImageWithAI(bitmap: Bitmap) {
        viewModelScope.launch {
            _cameraScanState.value = UiState.Loading
            val key = userProfile.value?.geminiApiKey ?: ""
            val result = GeminiService.scanCameraImage(bitmap, key)
            if (result != null) {
                _cameraScanState.value = UiState.Success(result)
            } else {
                _cameraScanState.value = UiState.Error("AI Vision Scan empty or failed. Make sure the food is clear and API key is set.")
            }
        }
    }

    // AI Recipe Ingredients Text Analyzer
    fun analyzeRecipeTextWithAI(recipeString: String) {
        viewModelScope.launch {
            _recipeState.value = UiState.Loading
            val key = userProfile.value?.geminiApiKey ?: ""
            val result = GeminiService.analyzeRecipeIngredients(recipeString, key)
            if (result != null) {
                _recipeState.value = UiState.Success(result)
            } else {
                _recipeState.value = UiState.Error("Failed to analyze recipe. Please verify inputs or API key.")
            }
        }
    }

    // Fast copy yesterday's meals
    fun duplicateYesterdayMeals() {
        viewModelScope.launch {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DATE, -1)
            val yesterdayStr = sdf.format(cal.time)

            // Read yesterday's log list manually using repository getFoodsByDate
            val yesterdayLogs = repository.getFoodsByDate(yesterdayStr).first()
            if (yesterdayLogs.isNotEmpty()) {
                yesterdayLogs.forEach { log ->
                    repository.insertFood(
                        log.copy(
                            id = 0, // Generate new PK
                            date = _selectedDate.value,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }
}

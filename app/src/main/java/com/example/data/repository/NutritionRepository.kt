package com.example.data.repository

import com.example.data.dao.*
import com.example.data.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NutritionRepository(
    private val userDao: UserDao,
    private val foodLogDao: FoodLogDao,
    private val exerciseDao: ExerciseDao,
    private val weightDao: WeightDao,
    private val waterDao: WaterDao,
    private val frequentFoodDao: FrequentFoodDao,
    private val stepDao: StepDao
) {
    // User Profile
    val userProfileFlow: Flow<UserProfile?> = userDao.getUserProfileFlow()

    suspend fun getUserProfile(): UserProfile? = withContext(Dispatchers.IO) {
        userDao.getUserProfile()
    }

    suspend fun saveUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        userDao.insertOrUpdateProfile(profile)
    }

    suspend fun updateWeight(newWeight: Double) = withContext(Dispatchers.IO) {
        userDao.updateWeight(newWeight)
        // Also log this in weight history
        val today = getTodayDateString()
        weightDao.insertWeight(WeightLog(date = today, weightKg = newWeight, notes = "Updated on profile"))
    }

    suspend fun updateGeminiApiKey(key: String) = withContext(Dispatchers.IO) {
        userDao.updateGeminiApiKey(key)
    }

    suspend fun updateDarkMode(darkMode: Boolean) = withContext(Dispatchers.IO) {
        userDao.updateDarkMode(darkMode)
    }

    // Food Logs
    fun getFoodsByDate(date: String): Flow<List<FoodLog>> = foodLogDao.getFoodsByDate(date)

    val favoriteFoodsFlow: Flow<List<FoodLog>> = foodLogDao.getFavoriteFoods()

    suspend fun insertFood(foodLog: FoodLog) = withContext(Dispatchers.IO) {
        foodLogDao.insertFood(foodLog)
        
        // Log to frequent foods
        val existing = frequentFoodDao.getFrequentFoodByName(foodLog.foodName)
        if (existing != null) {
            frequentFoodDao.incrementUseCount(foodLog.foodName)
        } else {
            frequentFoodDao.insertFrequentFood(
                FrequentFood(
                    foodName = foodLog.foodName,
                    avgCalories = foodLog.calories,
                    avgProtein = foodLog.proteinG,
                    useCount = 1
                )
            )
        }
    }

    suspend fun updateFood(foodLog: FoodLog) = withContext(Dispatchers.IO) {
        foodLogDao.updateFood(foodLog)
    }

    suspend fun deleteFood(foodLog: FoodLog) = withContext(Dispatchers.IO) {
        foodLogDao.deleteFood(foodLog)
    }

    suspend fun deleteFoodById(id: Long) = withContext(Dispatchers.IO) {
        foodLogDao.deleteFoodById(id)
    }

    // Exercises
    fun getExercisesByDate(date: String): Flow<List<ExerciseLog>> = exerciseDao.getExercisesByDate(date)
    
    val allExercisesFlow: Flow<List<ExerciseLog>> = exerciseDao.getAllExercises()

    suspend fun insertExercise(exerciseLog: ExerciseLog) = withContext(Dispatchers.IO) {
        exerciseDao.insertExercise(exerciseLog)
    }

    suspend fun deleteExercise(exerciseLog: ExerciseLog) = withContext(Dispatchers.IO) {
        exerciseDao.deleteExercise(exerciseLog)
    }

    // Body Weight
    val weightHistoryFlow: Flow<List<WeightLog>> = weightDao.getWeightHistory()

    suspend fun insertWeight(weightLog: WeightLog) = withContext(Dispatchers.IO) {
        weightDao.insertWeight(weightLog)
        // Also update the current profile weight
        userDao.updateWeight(weightLog.weightKg)
    }

    suspend fun deleteWeightById(id: Long) = withContext(Dispatchers.IO) {
        weightDao.deleteWeightById(id)
    }

    // Water
    fun getWaterByDateFlow(date: String): Flow<WaterLog?> = waterDao.getWaterByDateFlow(date)

    suspend fun incrementWater(date: String) = withContext(Dispatchers.IO) {
        val current = waterDao.getWaterByDate(date)
        val count = (current?.glassesCount ?: 0) + 1
        waterDao.insertOrUpdateWater(WaterLog(date = date, glassesCount = count, updatedAt = System.currentTimeMillis()))
    }

    suspend fun decrementWater(date: String) = withContext(Dispatchers.IO) {
        val current = waterDao.getWaterByDate(date) ?: return@withContext
        val count = (current.glassesCount - 1).coerceAtLeast(0)
        waterDao.insertOrUpdateWater(WaterLog(date = date, glassesCount = count, updatedAt = System.currentTimeMillis()))
    }

    // Frequent foods
    val frequentFoodsFlow: Flow<List<FrequentFood>> = frequentFoodDao.getFrequentFoods()

    // Steps / Pedometer
    fun getStepsByDateFlow(date: String): Flow<StepLog?> = stepDao.getStepsByDateFlow(date)

    suspend fun getStepsByDate(date: String): StepLog? = stepDao.getStepsByDate(date)

    suspend fun updateSteps(date: String, steps: Int, target: Int = 10000) = withContext(Dispatchers.IO) {
        val calories = steps * 0.04 // Standard average estimate: 0.04 calories burned per step
        val log = StepLog(
            date = date,
            steps = steps,
            targetSteps = target,
            caloriesBurned = calories,
            updatedAt = System.currentTimeMillis()
        )
        stepDao.insertOrUpdateStep(log)
    }

    private fun getTodayDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}

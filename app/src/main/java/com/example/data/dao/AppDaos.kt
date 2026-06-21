package com.example.data.dao

import androidx.room.*
import com.example.data.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)

    @Query("UPDATE user_profile SET weightKg = :newWeight WHERE id = 1")
    suspend fun updateWeight(newWeight: Double)

    @Query("UPDATE user_profile SET geminiApiKey = :newKey WHERE id = 1")
    suspend fun updateGeminiApiKey(newKey: String)

    @Query("UPDATE user_profile SET darkMode = :darkMode WHERE id = 1")
    suspend fun updateDarkMode(darkMode: Boolean)
}

@Dao
interface FoodLogDao {
    @Query("SELECT * FROM food_log WHERE date = :date ORDER BY id DESC")
    fun getFoodsByDate(date: String): Flow<List<FoodLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(foodLog: FoodLog)

    @Update
    suspend fun updateFood(foodLog: FoodLog)

    @Delete
    suspend fun deleteFood(foodLog: FoodLog)

    @Query("DELETE FROM food_log WHERE id = :id")
    suspend fun deleteFoodById(id: Long)

    @Query("SELECT * FROM food_log WHERE isFavorite = 1 GROUP BY foodName ORDER BY id DESC")
    fun getFavoriteFoods(): Flow<List<FoodLog>>
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise_log WHERE date = :date ORDER BY id DESC")
    fun getExercisesByDate(date: String): Flow<List<ExerciseLog>>

    @Query("SELECT * FROM exercise_log ORDER BY date DESC, id DESC LIMIT 100")
    fun getAllExercises(): Flow<List<ExerciseLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exerciseLog: ExerciseLog)

    @Delete
    suspend fun deleteExercise(exerciseLog: ExerciseLog)
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_log ORDER BY date ASC, id ASC")
    fun getWeightHistory(): Flow<List<WeightLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weightLog: WeightLog)

    @Query("DELETE FROM weight_log WHERE id = :id")
    suspend fun deleteWeightById(id: Long)
}

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_log WHERE date = :date LIMIT 1")
    fun getWaterByDateFlow(date: String): Flow<WaterLog?>

    @Query("SELECT * FROM water_log WHERE date = :date LIMIT 1")
    suspend fun getWaterByDate(date: String): WaterLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWater(waterLog: WaterLog)
}

@Dao
interface FrequentFoodDao {
    @Query("SELECT * FROM frequent_foods ORDER BY useCount DESC LIMIT 20")
    fun getFrequentFoods(): Flow<List<FrequentFood>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFrequentFood(frequentFood: FrequentFood)

    @Query("SELECT * FROM frequent_foods WHERE foodName = :foodName LIMIT 1")
    suspend fun getFrequentFoodByName(foodName: String): FrequentFood?

    @Query("UPDATE frequent_foods SET useCount = useCount + 1 WHERE foodName = :foodName")
    suspend fun incrementUseCount(foodName: String)
}

@Dao
interface StepDao {
    @Query("SELECT * FROM step_log WHERE date = :date LIMIT 1")
    fun getStepsByDateFlow(date: String): Flow<StepLog?>

    @Query("SELECT * FROM step_log WHERE date = :date LIMIT 1")
    suspend fun getStepsByDate(date: String): StepLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStep(stepLog: StepLog)
}


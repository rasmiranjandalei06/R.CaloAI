package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Only 1 profile exists
    val name: String,
    val age: Int,
    val gender: String, // Male, Female, Other
    val heightCm: Double,
    val weightKg: Double,
    val goal: String, // "Fat Loss", "Muscle Gain", "Maintain Weight"
    val activityLevel: String, // "Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Extremely Active"
    val targetCalories: Int,
    val targetProtein: Int,
    val targetCarbs: Int,
    val targetFat: Int,
    val waterTargetMl: Double,
    val unitSystem: String = "Metric", // Metric or Imperial
    val geminiApiKey: String = "",
    val darkMode: Boolean = false,
    val fastingMode: Boolean = false,
    val fastingStartTime: Long = 0L,
    val fastingDurationHours: Int = 16,
    val calorieRolloverConfig: Boolean = false,
    val targetWeightKg: Double = 70.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "food_log")
data class FoodLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // "yyyy-MM-dd"
    val mealType: String, // BREAKFAST, LUNCH, DINNER, SNACKS
    val foodName: String,
    val quantity: String,
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double = 0.0,
    val sugarG: Double = 0.0,
    val sodiumMg: Double = 0.0,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "exercise_log")
data class ExerciseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // "yyyy-MM-dd"
    val exerciseType: String, // Cardio, Strength, Yoga, Sports, Other
    val durationMin: Int,
    val intensity: String, // Low, Medium, High
    val caloriesBurned: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "weight_log")
data class WeightLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // "yyyy-MM-dd"
    val weightKg: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "water_log")
data class WaterLog(
    @PrimaryKey val date: String, // "yyyy-MM-dd"
    val glassesCount: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "frequent_foods")
data class FrequentFood(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodName: String,
    val avgCalories: Double,
    val avgProtein: Double,
    val useCount: Int
)

@Entity(tableName = "step_log")
data class StepLog(
    @PrimaryKey val date: String, // "yyyy-MM-dd"
    val steps: Int,
    val targetSteps: Int = 10000,
    val caloriesBurned: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)

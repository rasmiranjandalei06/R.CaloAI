package com.example.utils

import kotlin.math.roundToInt

data class CalculationResults(
    val bmi: Double,
    val bmiCategory: String,
    val idealWeightKg: Double,
    val idealWeightMin: Double,
    val idealWeightMax: Double,
    val bmr: Double,
    val tdee: Double,
    val targetCalories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val tdeeRangeMin: Int,
    val tdeeRangeMax: Int,
    val waterMl: Double,
    val waterGlasses: Int
)

object CalorieCalculator {

    fun calculateEverything(
        age: Int,
        gender: String, // "Male", "Female", "Other"
        heightCm: Double,
        weightKg: Double,
        goal: String, // "Fat Loss", "Muscle Gain", "Maintain Weight"
        activityLevel: String // "Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Extremely Active"
    ): CalculationResults {
        // 1. BMI
        val heightM = heightCm / 100.0
        val bmi = if (heightM > 0) weightKg / (heightM * heightM) else 0.0
        val bmiCategory = when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal"
            bmi < 30.0 -> "Overweight"
            else -> "Obese"
        }

        // 2. Ideal Body Weight (Devine Formula)
        // height in inches = cm / 2.54
        val heightInches = heightCm / 2.54
        val heightDiffFrom60 = (heightInches - 60.0).coerceAtLeast(0.0)
        
        val isMale = gender.lowercase().contains("male")
        val baseIbw = if (isMale) 50.0 else 45.5
        val idealWeightKg = baseIbw + (2.3 * heightDiffFrom60)
        
        val idealWeightMin = idealWeightKg * 0.90
        val idealWeightMax = idealWeightKg * 1.10

        // 3. Mifflin-St Jeor BMR
        val bmr = if (isMale) {
            (10 * weightKg) + (6.25 * heightCm) - (5 * age) + 5
        } else {
            (10 * weightKg) + (6.25 * heightCm) - (5 * age) - 161
        }

        // Activity Multiplier
        val multiplier = when (activityLevel) {
            "Sedentary" -> 1.2
            "Lightly Active" -> 1.375
            "Moderately Active" -> 1.55
            "Very Active" -> 1.725
            "Extremely Active" -> 1.9
            else -> 1.2
        }

        val tdee = bmr * multiplier

        // Goal adjustments
        var targetCalories = tdee.roundToInt()
        var calorieRangeMin = targetCalories - 100
        var calorieRangeMax = targetCalories + 100

        when (goal) {
            "Fat Loss" -> {
                targetCalories = (tdee - 500).roundToInt().coerceAtLeast(1200)
                calorieRangeMin = (tdee - 700).roundToInt().coerceAtLeast(1000)
                calorieRangeMax = (tdee - 300).roundToInt().coerceAtLeast(1200)
            }
            "Muscle Gain" -> {
                targetCalories = (tdee + 300).roundToInt()
                calorieRangeMin = targetCalories - 150
                calorieRangeMax = targetCalories + 150
            }
            "Maintain Weight", "Maintain" -> {
                targetCalories = tdee.roundToInt()
                calorieRangeMin = targetCalories - 100
                calorieRangeMax = targetCalories + 100
            }
        }

        // 4. Macro break down
        // Fat Loss: 40% Protein / 30% Carbs / 30% Fat
        // Muscle Gain: 35% Protein / 45% Carbs / 20% Fat
        // Maintain: 30% Protein / 40% Carbs / 30% Fat
        // 1g Protein = 4 kcal, 1g Carb = 4 kcal, 1g Fat = 9 kcal
        val (proteinPct, carbsPct, fatPct) = when (goal) {
            "Fat Loss" -> Triple(0.40, 0.30, 0.30)
            "Muscle Gain" -> Triple(0.35, 0.45, 0.20)
            else -> Triple(0.30, 0.40, 0.30) // Maintain
        }

        val proteinGrams = ((targetCalories * proteinPct) / 4.0).roundToInt()
        val carbsGrams = ((targetCalories * carbsPct) / 4.0).roundToInt()
        val fatGrams = ((targetCalories * fatPct) / 9.0).roundToInt()

        // 5. Water intake
        // Formula: Weight in kg * 35 ml
        val waterMl = weightKg * 35.0
        val waterGlasses = (waterMl / 250.0).roundToInt().coerceAtLeast(4)

        return CalculationResults(
            bmi = bmi,
            bmiCategory = bmiCategory,
            idealWeightKg = idealWeightKg,
            idealWeightMin = idealWeightMin,
            idealWeightMax = idealWeightMax,
            bmr = bmr,
            tdee = tdee,
            targetCalories = targetCalories,
            proteinGrams = proteinGrams,
            carbsGrams = carbsGrams,
            fatGrams = fatGrams,
            tdeeRangeMin = calorieRangeMin,
            tdeeRangeMax = calorieRangeMax,
            waterMl = waterMl,
            waterGlasses = waterGlasses
        )
    }

    // Body fat US Navy method
    // For Males: 86.010 * log10(waist - neck) - 70.041 * log10(height) + 36.76
    // For Females: 163.205 * log10(waist + hip - neck) - 97.684 * log10(height) - 78.387
    // Standard inputs are in inches
    fun estimateBodyFat(
        waistCm: Double,
        neckCm: Double,
        hipCm: Double,
        heightCm: Double,
        gender: String
    ): Double {
        val waistIn = waistCm / 2.54
        val neckIn = neckCm / 2.54
        val hipIn = hipCm / 2.54
        val heightIn = heightCm / 2.54

        return if (gender.lowercase().contains("male")) {
            val val1 = waistIn - neckIn
            if (val1 <= 0) return 15.0
            val result = 86.010 * kotlin.math.log10(val1) - 70.041 * kotlin.math.log10(heightIn) + 36.76
            result.coerceIn(3.0, 50.0)
        } else {
            val val1 = waistIn + hipIn - neckIn
            if (val1 <= 0) return 22.0
            val result = 163.205 * kotlin.math.log10(val1) - 97.684 * kotlin.math.log10(heightIn) - 78.387
            result.coerceIn(5.0, 60.0)
        }
    }
}

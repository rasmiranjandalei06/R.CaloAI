package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.repository.NutritionRepository

class NutriTrackApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        NutritionRepository(
            database.userDao(),
            database.foodLogDao(),
            database.exerciseDao(),
            database.weightDao(),
            database.waterDao(),
            database.frequentFoodDao(),
            database.stepDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: NutriTrackApp
            private set
    }
}

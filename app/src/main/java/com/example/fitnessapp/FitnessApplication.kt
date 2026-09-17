package com.example.fitnessapp

import android.app.Application
import com.example.fitnessapp.data.network.GeminiVisionService
import com.example.fitnessapp.data.repository.AppFitnessRepository
import com.example.fitnessapp.data.repository.FitnessRepository

class FitnessApplication : Application() {

    lateinit var repository: FitnessRepository
        private set

    lateinit var geminiVisionService: GeminiVisionService
        private set

    lateinit var foodNutritionSearchService: com.example.fitnessapp.data.nutrition.FoodNutritionSearchService
        private set

    lateinit var openFoodFactsService: com.example.fitnessapp.data.nutrition.OpenFoodFactsService
        private set

    lateinit var stepTrackerManager: com.example.fitnessapp.data.sensor.StepTrackerManager
        private set

    lateinit var syncCoordinator: com.example.fitnessapp.data.sync.SyncCoordinator
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = AppFitnessRepository(this)
        geminiVisionService = GeminiVisionService()
        foodNutritionSearchService = com.example.fitnessapp.data.nutrition.FoodNutritionSearchService(this)
        openFoodFactsService = com.example.fitnessapp.data.nutrition.OpenFoodFactsService()
        stepTrackerManager = com.example.fitnessapp.data.sensor.StepTrackerManager(this)
        syncCoordinator = com.example.fitnessapp.data.sync.SyncCoordinator(this, repository)
        syncCoordinator.start()
        com.example.fitnessapp.data.notification.FitnessNotificationHelper.createNotificationChannels(this)
        com.example.fitnessapp.data.notification.ReminderScheduler.rescheduleAll(this)
    }

    companion object {
        lateinit var instance: FitnessApplication
            private set
    }
}

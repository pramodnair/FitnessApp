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

    lateinit var syncCoordinator: com.example.fitnessapp.data.sync.SyncCoordinator
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = AppFitnessRepository(this)
        geminiVisionService = GeminiVisionService()
        syncCoordinator = com.example.fitnessapp.data.sync.SyncCoordinator(this, repository)
        syncCoordinator.start()
    }

    companion object {
        lateinit var instance: FitnessApplication
            private set
    }
}

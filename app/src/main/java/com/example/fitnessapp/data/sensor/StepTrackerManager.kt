package com.example.fitnessapp.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StepTrackerManager(private val context: Context) : SensorEventListener {

    private val prefs = context.getSharedPreferences("step_tracker_prefs", Context.MODE_PRIVATE)
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepCounterSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val _todaySteps = MutableStateFlow(loadTodaySteps())
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    private val _caloriesBurned = MutableStateFlow(calculateBurnedCalories(_todaySteps.value, 70f))
    val caloriesBurned: StateFlow<Int> = _caloriesBurned.asStateFlow()

    private var userWeightKg: Float = 70f
    private var isRegistered = false

    init {
        startTracking()
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun loadTodaySteps(): Int {
        val today = getTodayDate()
        return prefs.getInt("steps_$today", 0)
    }

    fun updateUserWeight(weightKg: Float) {
        if (weightKg > 20f) {
            userWeightKg = weightKg
            _caloriesBurned.value = calculateBurnedCalories(_todaySteps.value, userWeightKg)
        }
    }

    fun startTracking() {
        if (isRegistered || sensorManager == null) return
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
            isRegistered = true
            Log.d("StepTrackerManager", "Registered TYPE_STEP_COUNTER sensor")
        } else if (stepDetectorSensor != null) {
            sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI)
            isRegistered = true
            Log.d("StepTrackerManager", "Registered TYPE_STEP_DETECTOR fallback sensor")
        } else {
            Log.w("StepTrackerManager", "No step sensor available on this hardware")
        }
    }

    fun stopTracking() {
        if (isRegistered && sensorManager != null) {
            sensorManager.unregisterListener(this)
            isRegistered = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val today = getTodayDate()

        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalRawSteps = event.values[0].toInt()
            val savedBaseline = prefs.getInt("baseline_$today", -1)

            val currentBaseline = if (savedBaseline == -1 || totalRawSteps < savedBaseline) {
                // First event of the day or reboot occurred
                prefs.edit().putInt("baseline_$today", totalRawSteps).apply()
                totalRawSteps
            } else {
                savedBaseline
            }

            val stepsFromSensor = (totalRawSteps - currentBaseline).coerceAtLeast(0)
            val manualOffset = prefs.getInt("manual_offset_$today", 0)
            val computedSteps = stepsFromSensor + manualOffset

            updateSteps(computedSteps, today)
        } else if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            // Incremental 1 step per event
            val current = loadTodaySteps() + 1
            updateSteps(current, today)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun addManualSteps(count: Int) {
        val today = getTodayDate()
        val currentOffset = prefs.getInt("manual_offset_$today", 0)
        val newOffset = currentOffset + count
        prefs.edit().putInt("manual_offset_$today", newOffset).apply()

        val current = _todaySteps.value + count
        updateSteps(current, today)
    }

    private fun updateSteps(steps: Int, today: String) {
        _todaySteps.value = steps
        prefs.edit().putInt("steps_$today", steps).apply()
        _caloriesBurned.value = calculateBurnedCalories(steps, userWeightKg)
    }

    companion object {
        fun calculateBurnedCalories(steps: Int, weightKg: Float): Int {
            // Standard metabolic equivalent formula: ~0.04 kcal per step for 70kg, scales linearly with weight
            val factor = (weightKg / 70f) * 0.04f
            return (steps * factor).toInt()
        }
    }
}

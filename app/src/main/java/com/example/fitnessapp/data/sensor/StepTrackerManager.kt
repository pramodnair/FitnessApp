package com.example.fitnessapp.data.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
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

    fun loadTodaySteps(): Int {
        val today = getTodayDate()
        return prefs.getInt("steps_$today", 0)
    }

    fun refreshTodaySteps() {
        val today = getTodayDate()
        val steps = prefs.getInt("steps_$today", 0)
        _todaySteps.value = steps
        _caloriesBurned.value = calculateBurnedCalories(steps, userWeightKg)
    }

    fun updateUserWeight(weightKg: Float) {
        if (weightKg > 20f) {
            userWeightKg = weightKg
            _caloriesBurned.value = calculateBurnedCalories(_todaySteps.value, userWeightKg)
        }
    }

    fun startTracking() {
        if (sensorManager == null) {
            Log.w(TAG, "SensorManager unavailable")
            return
        }

        // Validate runtime permission on Android 10+ (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                Log.w(TAG, "Cannot start tracking: ACTIVITY_RECOGNITION not granted")
                isRegistered = false
                return
            }
        }

        // Clean unregister first to avoid duplicate callbacks or stale state
        if (isRegistered) {
            try {
                sensorManager.unregisterListener(this)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering listener: ${e.message}")
            }
            isRegistered = false
        }

        var anyRegistered = false

        // Register TYPE_STEP_COUNTER (delivers cumulative hardware steps since boot)
        if (stepCounterSensor != null) {
            val success = sensorManager.registerListener(
                this,
                stepCounterSensor,
                SensorManager.SENSOR_DELAY_UI
            )
            Log.d(TAG, "Registered TYPE_STEP_COUNTER: $success")
            if (success) anyRegistered = true
        }

        // Also register TYPE_STEP_DETECTOR (delivers instant real-time events on every single step)
        if (stepDetectorSensor != null) {
            val success = sensorManager.registerListener(
                this,
                stepDetectorSensor,
                SensorManager.SENSOR_DELAY_UI
            )
            Log.d(TAG, "Registered TYPE_STEP_DETECTOR: $success")
            if (success) anyRegistered = true
        }

        isRegistered = anyRegistered
        if (!anyRegistered) {
            Log.w(TAG, "Failed to register step sensors")
        }
    }

    fun stopTracking() {
        if (isRegistered && sensorManager != null) {
            try {
                sensorManager.unregisterListener(this)
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping tracking: ${e.message}")
            }
            isRegistered = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val today = getTodayDate()

        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalRawSteps = event.values[0].toInt()
            val savedBaseline = prefs.getInt("baseline_$today", -1)

            val currentBaseline = if (savedBaseline == -1) {
                // First event of the day: establish baseline
                prefs.edit().putInt("baseline_$today", totalRawSteps).apply()
                totalRawSteps
            } else if (totalRawSteps < savedBaseline) {
                // Device rebooted: preserve previously accumulated steps
                val currentSteps = _todaySteps.value
                prefs.edit()
                    .putInt("manual_offset_$today", currentSteps)
                    .putInt("baseline_$today", totalRawSteps)
                    .apply()
                totalRawSteps
            } else {
                savedBaseline
            }

            val stepsFromSensor = (totalRawSteps - currentBaseline).coerceAtLeast(0)
            val manualOffset = prefs.getInt("manual_offset_$today", 0)
            val computedSteps = maxOf(stepsFromSensor + manualOffset, _todaySteps.value)

            Log.d(TAG, "TYPE_STEP_COUNTER updated: $computedSteps (raw=$totalRawSteps, baseline=$currentBaseline, offset=$manualOffset)")
            updateSteps(computedSteps, today)
        } else if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            // Instant incremental event: 1 step detected by hardware
            val nextSteps = _todaySteps.value + 1
            Log.d(TAG, "TYPE_STEP_DETECTOR increment: $nextSteps")
            updateSteps(nextSteps, today)
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
        private const val TAG = "StepTrackerManager"

        fun calculateBurnedCalories(steps: Int, weightKg: Float): Int {
            // Standard metabolic equivalent formula: ~0.04 kcal per step for 70kg, scales linearly with weight
            val factor = (weightKg / 70f) * 0.04f
            return (steps * factor).toInt()
        }
    }
}

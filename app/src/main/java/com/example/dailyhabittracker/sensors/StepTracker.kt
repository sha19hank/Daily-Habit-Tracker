package com.example.dailyhabittracker.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.dailyhabittracker.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.time.LocalDate

class StepTracker(
    private val context: Context,
    private val settings: SettingsRepository
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val trackerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun stepState(): Flow<StepState> = callbackFlow {
        val counter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val detector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        if (counter == null && detector == null) {
            trySend(StepState(false, 0))
            close()
            return@callbackFlow
        }

        var currentBaseline: Int? = null
        var stepsToday = 0
        var baselineLoaded = false

        trackerScope.launch {
            val today = LocalDate.now()
            if (counter != null) {
                currentBaseline = settings.getStepBaseline(today)
                baselineLoaded = true
            } else if (detector != null) {
                stepsToday = settings.getStepCount(today) ?: 0
                baselineLoaded = true
                trySend(StepState(true, stepsToday))
            }
        }

        var lastSavedSteps = 0
        var lastSaveTime = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (!baselineLoaded) return
                
                val today = LocalDate.now()
                val now = System.currentTimeMillis()

                if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                    val total = event.values.firstOrNull()?.toInt() ?: return
                    if (currentBaseline == null) {
                        currentBaseline = total
                        trackerScope.launch { settings.setStepBaseline(today, total) }
                    }
                    val baseline = currentBaseline!!
                    stepsToday = (total - baseline).coerceAtLeast(0)
                    trySend(StepState(true, stepsToday))
                } else if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                    stepsToday += 1
                    trySend(StepState(true, stepsToday))
                    
                    if (stepsToday - lastSavedSteps >= 10 || now - lastSaveTime > 10000) {
                        val toSave = stepsToday
                        trackerScope.launch { settings.setStepCount(today, toSave) }
                        lastSavedSteps = toSave
                        lastSaveTime = now
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val sensor = counter ?: detector
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }.flowOn(Dispatchers.Default)
}

data class StepState(
    val supported: Boolean,
    val stepsToday: Int
)

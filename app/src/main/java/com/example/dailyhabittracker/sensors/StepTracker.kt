package com.example.dailyhabittracker.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.dailyhabittracker.data.SettingsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate

class StepTracker(
    private val context: Context,
    private val settings: SettingsRepository
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun stepState(): Flow<StepState> = callbackFlow {
        val counter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val detector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        if (counter == null && detector == null) {
            trySend(StepState(false, 0))
            close()
            return@callbackFlow
        }

        var lastCount = 0

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val today = LocalDate.now()
                if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                    val total = event.values.firstOrNull()?.toInt() ?: return
                    val baseline = runBlockingBaseline(today, total)
                    val steps = (total - baseline).coerceAtLeast(0)
                    lastCount = steps
                    trySend(StepState(true, steps))
                } else if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                    val updated = runBlockingIncrement(today)
                    lastCount = updated
                    trySend(StepState(true, updated))
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val sensor = counter ?: detector
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)

        trySend(StepState(true, lastCount))

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }.flowOn(Dispatchers.Default)

    private fun runBlockingBaseline(date: LocalDate, total: Int): Int {
        return kotlinx.coroutines.runBlocking {
            val stored = settings.getStepBaseline(date)
            if (stored != null) return@runBlocking stored
            settings.setStepBaseline(date, total)
            total
        }
    }

    private fun runBlockingIncrement(date: LocalDate): Int {
        return kotlinx.coroutines.runBlocking {
            val current = settings.getStepCount(date) ?: 0
            val updated = current + 1
            settings.setStepCount(date, updated)
            updated
        }
    }
}

data class StepState(
    val supported: Boolean,
    val stepsToday: Int
)

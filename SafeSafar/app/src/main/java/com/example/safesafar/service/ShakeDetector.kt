package com.example.safesafar.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(context: Context, private val onShake: () -> Unit) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var shakeCount = 0
    private var lastShakeTime: Long = 0
    private val SHAKE_THRESHOLD = 12.0f
    private val SHAKE_TIMEOUT = 2000L // 2 seconds inactivity
    private val SHAKE_DEBOUNCE = 300L

    private val gravity = FloatArray(3)
    private val linear_acceleration = FloatArray(3)

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val alpha = 0.8f

        // Apply low-pass filter to smooth readings and isolate gravity
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

        // High-pass filter to remove gravity
        linear_acceleration[0] = event.values[0] - gravity[0]
        linear_acceleration[1] = event.values[1] - gravity[1]
        linear_acceleration[2] = event.values[2] - gravity[2]

        val x = linear_acceleration[0]
        val y = linear_acceleration[1]
        val z = linear_acceleration[2]

        val shakeForce = sqrt(x * x + y * y + z * z)

        val now = System.currentTimeMillis()

        // Reset shakeCount after 2 seconds inactivity
        if (shakeCount > 0 && (now - lastShakeTime) > SHAKE_TIMEOUT) {
            shakeCount = 0
        }

        if (shakeForce > SHAKE_THRESHOLD) {
            // Debounce: ignore shakes within 300ms interval
            if (now - lastShakeTime > SHAKE_DEBOUNCE || shakeCount == 0) { 
                lastShakeTime = now
                shakeCount++
                
                if (shakeCount == 5) {
                    onShake()
                    shakeCount = 0
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

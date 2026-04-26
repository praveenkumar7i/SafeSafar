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
    private val SHAKE_TIMEOUT = 3000L

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val force = sqrt(x * x + y * y + z * z)

        if (force > SHAKE_THRESHOLD) {
            val now = System.currentTimeMillis()
            
            if (shakeCount > 0 && (now - lastShakeTime) > SHAKE_TIMEOUT) {
                shakeCount = 0
            }

            if (now - lastShakeTime > 300 || shakeCount == 0) { 
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

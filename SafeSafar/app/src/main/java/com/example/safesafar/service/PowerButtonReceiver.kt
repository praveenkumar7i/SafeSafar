package com.example.safesafar.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PowerButtonReceiver(private val onTrigger: () -> Unit) : BroadcastReceiver() {
    private var pressCount = 0
    private var lastPressTime = 0L

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_ON || intent.action == Intent.ACTION_SCREEN_OFF) {
            val now = System.currentTimeMillis()
            if (now - lastPressTime > 3000) {
                pressCount = 0 // Reset if more than 3 seconds between presses
            }
            lastPressTime = now
            pressCount++
            
            if (pressCount >= 3) {
                onTrigger()
                pressCount = 0
            }
        }
    }
}

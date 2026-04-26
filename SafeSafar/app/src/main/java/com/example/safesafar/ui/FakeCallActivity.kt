package com.example.safesafar.ui

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.safesafar.R

class FakeCallActivity : AppCompatActivity() {

    private var ringtone: android.media.Ringtone? = null
    private var vibrator: Vibrator? = null
    private var timerHandler = Handler(Looper.getMainLooper())
    private var secondsRecorded = 0
    private var timerRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fake_call)

        val callerName = findViewById<TextView>(R.id.tvCallerName)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvAvatarLetter = findViewById<TextView>(R.id.tvAvatarLetter)
        val layoutIncomingCall = findViewById<LinearLayout>(R.id.layoutIncomingCall)
        val layoutOngoingCall = findViewById<LinearLayout>(R.id.layoutOngoingCall)
        val tvTimer = findViewById<TextView>(R.id.tvTimer)
        val btnAnswer = findViewById<View>(R.id.btnAnswer)
        val btnDecline = findViewById<View>(R.id.btnDecline)
        val btnEndCall = findViewById<View>(R.id.btnEndCall)

        val name = intent.getStringExtra("CALLER_NAME") ?: "Police Control Room"
        callerName.text = name
        tvAvatarLetter.text = name.firstOrNull()?.toString()?.uppercase() ?: "P"
        tvStatus.text = "Calling..."

        startRingingAndVibration()

        // Simple pulse animation on Answer button
        btnAnswer.animate().scaleX(1.1f).scaleY(1.1f).setDuration(500).withEndAction(object : Runnable {
            override fun run() {
                btnAnswer.animate().scaleX(1.0f).scaleY(1.0f).setDuration(500).withEndAction(this).start()
            }
        }).start()

        btnAnswer.setOnClickListener {
            stopRingingAndVibration()
            tvStatus.text = "Call in progress"
            
            // Fade out incoming, fade in ongoing
            layoutIncomingCall.animate().alpha(0f).setDuration(300).withEndAction {
                layoutIncomingCall.visibility = View.GONE
            }.start()
            
            layoutOngoingCall.alpha = 0f
            layoutOngoingCall.visibility = View.VISIBLE
            layoutOngoingCall.animate().alpha(1f).setDuration(300).start()
            
            startTimer(tvTimer)
        }

        btnDecline.setOnClickListener {
            stopRingingAndVibration()
            finish()
        }

        btnEndCall.setOnClickListener {
            stopRingingAndVibration()
            timerRunnable?.let { timerHandler.removeCallbacks(it) }
            finish()
        }
    }

    private fun startTimer(tvTimer: TextView) {
        secondsRecorded = 0
        timerRunnable = object : Runnable {
            override fun run() {
                secondsRecorded++
                val mins = secondsRecorded / 60
                val secs = secondsRecorded % 60
                tvTimer.text = String.format("%02d:%02d", mins, secs)
                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.postDelayed(timerRunnable!!, 1000)
    }

    private fun startRingingAndVibration() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, uri)
            ringtone?.play()
    
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 0)
                vibrator?.vibrate(effect)
            } else {
                vibrator?.vibrate(longArrayOf(0, 1000, 1000), 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRingingAndVibration() {
        try {
            ringtone?.stop()
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingingAndVibration()
        timerRunnable?.let { timerHandler.removeCallbacks(it) }
    }
}
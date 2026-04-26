package com.example.safesafar.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.SmsManager
import android.util.Log
import android.media.MediaPlayer
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.example.safesafar.MainActivity
import com.example.safesafar.utils.ContactManager
import com.google.android.gms.location.LocationServices
import java.io.File

class SosService : Service() {

    private val CHANNEL_ID = "SafeSafarChannel"
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var powerButtonReceiver: PowerButtonReceiver
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null

    private var lastSosTime = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        shakeDetector = ShakeDetector(this) { 
            val prefs = getSharedPreferences("SafeSafarPrefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("shake_enabled", true)) {
                triggerSosRoutine()
            }
        }
        shakeDetector.start()

        powerButtonReceiver = PowerButtonReceiver { 
            val prefs = getSharedPreferences("SafeSafarPrefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("power_enabled", true)) {
                triggerSosRoutine()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(powerButtonReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafeSafar Active")
            .setContentText("Monitoring for Shake & Power Button SOS")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        if (intent?.action == "ACTION_TRIGGER_SOS") {
            val msg = intent.getStringExtra("CUSTOM_MSG") ?: ""
            val fromButton = intent.getBooleanExtra("FROM_BUTTON", false)
            triggerSosRoutine(msg, fromButton)
        } else if (intent?.action == "ACTION_RECORD_AUDIO") {
            startAudioRecording()
        } else if (intent?.action == "ACTION_SEND_FALLBACK_SMS") {
            getLocationAndSendSms("Help me! (Voice note shared separately)")
        }

        return START_STICKY
    }

    private fun triggerSosRoutine(customMsg: String = "", fromButton: Boolean = false) {
        val prefs = getSharedPreferences("SafeSafarPrefs", Context.MODE_PRIVATE)
        
        if (fromButton && !prefs.getBoolean("tap_enabled", true)) return
        
        val now = System.currentTimeMillis()
        if (now - lastSosTime < 10000) return // 10 seconds cooldown
        lastSosTime = now

        Log.d("SosService", "SOS Triggered!")
        vibrateDevice()
        
        if (prefs.getBoolean("auto_sms_enabled", true)) {
            getLocationAndSendSms(customMsg)
        }
        startAudioRecording()
        
        if (prefs.getBoolean("auto_fake_call_enabled", false)) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val fakeCallIntent = Intent(this, com.example.safesafar.ui.FakeCallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(fakeCallIntent)
            }, 10000)
        }

        if (prefs.getBoolean("siren_enabled", false)) {
            playSiren()
        }
    }

    private fun playSiren() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer.create(this, alarmUri)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                stopSiren()
            }, 10000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopSiren() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibrateDevice() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500, 200, 500), -1)
            vibrator.vibrate(effect)
        } else {
            vibrator.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
        }
    }

    private fun getLocationAndSendSms(customMsg: String) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    dispatchSms(location, customMsg)
                } else {
                    // Last known location unavailable, try current location request
                    val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 2000L
                    ).setMaxUpdates(1).build()
                    val callback = object : com.google.android.gms.location.LocationCallback() {
                        override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                            val loc = result.lastLocation
                            if (loc != null) dispatchSms(loc, customMsg)
                            else sendSms(buildMessage(customMsg, null))
                            fusedLocationClient.removeLocationUpdates(this)
                        }
                    }
                    fusedLocationClient.requestLocationUpdates(locationRequest, callback, android.os.Looper.getMainLooper())
                    // Failsafe: send without location after 4s if GPS is slow
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        sendSms(buildMessage(customMsg, null))
                        fusedLocationClient.removeLocationUpdates(callback)
                    }, 4000)
                }
            }.addOnFailureListener {
                sendSms(buildMessage(customMsg, null))
            }
        } catch (e: SecurityException) {
            sendSms(buildMessage(customMsg, null))
        }
    }

    private fun dispatchSms(location: Location, customMsg: String) {
        sendSms(buildMessage(customMsg, location))
    }

    private fun buildMessage(customMsg: String, location: Location?): String {
        val locString = if (location != null)
            "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        else
            "Location unavailable"
        val timestamp = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
            .format(java.util.Date())
        val base = if (customMsg.isNotBlank()) customMsg else "Help me! I am in danger."
        return "$base\nMy location:\n$locString\nTime: $timestamp"
    }

    private fun sendSms(message: String) {
        val contacts = ContactManager.getContacts(this)
        if (contacts.isEmpty()) return
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) getSystemService(SmsManager::class.java) else SmsManager.getDefault()
            for (contact in contacts) {
                smsManager.sendTextMessage(contact.phone, null, message, null, null)
            }
        } catch (e: Exception) {}
    }

    private fun startAudioRecording() {
        try {
            val file = File(cacheDir, "sos_audio_${System.currentTimeMillis()}.3gp")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ stopAudioRecording() }, 15000)
        } catch (e: Exception) {}
    }

    private fun stopAudioRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "SOS Service", NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        shakeDetector.stop()
        unregisterReceiver(powerButtonReceiver)
        stopAudioRecording()
        stopSiren()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

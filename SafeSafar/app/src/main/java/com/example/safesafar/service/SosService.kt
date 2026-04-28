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
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.safesafar.MainActivity
import com.example.safesafar.utils.ContactManager
import com.google.android.gms.location.LocationServices
import java.io.File

class SosService : Service() {

    private val CHANNEL_ID = "SafeSafarChannel"
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var powerButtonReceiver: PowerButtonReceiver
    private var mediaPlayer: MediaPlayer? = null

    private var lastSosTime = 0L

    private var sirenHandler: android.os.Handler? = null
    private var sirenRunnable: Runnable? = null
    private var flashHandler: android.os.Handler? = null
    private var flashRunnable: Runnable? = null

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
        val prefs = getSharedPreferences("SafeSafarPrefs", Context.MODE_PRIVATE)
        if (prefs.getString("pending_sos", null) != null) {
            startRetryLoop()
        }

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
        if (prefs.getBoolean("flashlight_enabled", false)) {
            startFlashlight()
        }
    }

    private fun playSiren() {
        try {
            stopSiren() // Stop any currently playing siren and remove old callbacks
            val prefs = getSharedPreferences("SafeSafarPrefs", Context.MODE_PRIVATE)
            val customUriString = prefs.getString("custom_siren_uri", null)
            val uri = if (customUriString != null) Uri.parse(customUriString) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC), 0)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@SosService, uri)
                setAudioStreamType(android.media.AudioManager.STREAM_MUSIC)
                isLooping = true
                prepare()
                start()
            }
            sirenHandler = android.os.Handler(android.os.Looper.getMainLooper())
            sirenRunnable = Runnable { stopSiren() }
            sirenHandler?.postDelayed(sirenRunnable!!, 15000)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                android.widget.Toast.makeText(this, "Invalid sound file", android.widget.Toast.LENGTH_SHORT).show()
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer.create(this, fallbackUri)
                mediaPlayer?.isLooping = true
                mediaPlayer?.start()
                sirenHandler = android.os.Handler(android.os.Looper.getMainLooper())
                sirenRunnable = Runnable { stopSiren() }
                sirenHandler?.postDelayed(sirenRunnable!!, 15000)
            } catch (ex: Exception) {}
        }
    }

    private fun stopSiren() {
        try {
            sirenRunnable?.let { sirenHandler?.removeCallbacks(it) }
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
                    var isResolved = false
                    val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 2000L
                    ).setMaxUpdates(1).build()
                    
                    val callback = object : com.google.android.gms.location.LocationCallback() {
                        override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                            if (!isResolved) {
                                isResolved = true
                                val loc = result.lastLocation
                                if (loc != null) dispatchSms(loc, customMsg)
                                else sendSms(buildMessage(customMsg, null))
                                fusedLocationClient.removeLocationUpdates(this)
                            }
                        }
                    }
                    fusedLocationClient.requestLocationUpdates(locationRequest, callback, android.os.Looper.getMainLooper())
                    
                    // Failsafe: send without location after 5s if GPS is slow
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (!isResolved) {
                            isResolved = true
                            sendSms(buildMessage(customMsg, null))
                            fusedLocationClient.removeLocationUpdates(callback)
                        }
                    }, 5000)
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
        val base = if (customMsg.isNotBlank()) customMsg else "🚨 EMERGENCY! I need help."
        return "$base\nMy location:\n$locString"
    }

    private fun hasCellularSignal(): Boolean {
        val isAirplaneMode = Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        if (isAirplaneMode) return false
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.simState == TelephonyManager.SIM_STATE_READY
    }

    private var isRetryLoopRunning = false
    private val retryHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private fun saveSosForRetry(message: String) {
        val prefs = getSharedPreferences("SafeSafarPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("pending_sos", message).apply()
    }

    private fun startRetryLoop() {
        if (isRetryLoopRunning) return
        isRetryLoopRunning = true
        val retryRunnable = object : Runnable {
            override fun run() {
                val prefs = getSharedPreferences("SafeSafarPrefs", Context.MODE_PRIVATE)
                val pendingSos = prefs.getString("pending_sos", null)
                if (pendingSos == null) {
                    isRetryLoopRunning = false
                    return
                }
                if (hasCellularSignal()) {
                    try {
                        val contacts = ContactManager.getContacts(this@SosService)
                        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) getSystemService(SmsManager::class.java) else SmsManager.getDefault()
                        val parts = smsManager.divideMessage(pendingSos)
                        for (contact in contacts) {
                            smsManager.sendMultipartTextMessage(contact.phone, null, parts, null, null)
                        }
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            Toast.makeText(this@SosService, "SOS sent successfully", Toast.LENGTH_SHORT).show()
                        }
                        prefs.edit().remove("pending_sos").apply()
                        isRetryLoopRunning = false
                        return
                    } catch (e: Exception) {}
                }
                retryHandler.postDelayed(this, 30000)
            }
        }
        retryHandler.postDelayed(retryRunnable, 30000)
    }

    private fun sendSms(message: String) {
        val contacts = ContactManager.getContacts(this)
        if (contacts.isEmpty()) return

        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(this, "Sending SOS via SMS...", Toast.LENGTH_SHORT).show()
        }

        if (!hasCellularSignal()) {
            saveSosForRetry(message)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(this, "No network. SOS will be sent when network is available", Toast.LENGTH_LONG).show()
            }
            startRetryLoop()
            return
        }

        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) getSystemService(SmsManager::class.java) else SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            for (contact in contacts) {
                smsManager.sendMultipartTextMessage(contact.phone, null, parts, null, null)
            }
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(this, "SOS sent successfully", Toast.LENGTH_SHORT).show()
            }
            val prefs = getSharedPreferences("SafeSafarPrefs", Context.MODE_PRIVATE)
            prefs.edit().remove("pending_sos").apply()
        } catch (e: Exception) {
            saveSosForRetry(message)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(this, "Will retry when network is available", Toast.LENGTH_SHORT).show()
            }
            startRetryLoop()
        }
    }

    private fun startFlashlight() {
        try {
            stopFlashlight() // Ensure any existing timeout is cancelled
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            
            cameraManager.setTorchMode(cameraId, true)
            
            flashHandler = android.os.Handler(android.os.Looper.getMainLooper())
            flashRunnable = Runnable { stopFlashlight() }
            flashHandler?.postDelayed(flashRunnable!!, 15000)
        } catch (e: Exception) {}
    }

    private fun stopFlashlight() {
        try {
            flashRunnable?.let { flashHandler?.removeCallbacks(it) }
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, false)
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
        stopSiren()
        stopFlashlight()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

package com.example.safesafar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.*
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.safesafar.databinding.ActivityMainBinding
import com.example.safesafar.service.SosService
import com.example.safesafar.ui.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQUEST_CODE = 100
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    private val timerHandler = Handler(Looper.getMainLooper())
    private var secondsRecorded = 0
    private var timerRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Make ticker scroll
        binding.tvTicker.isSelected = true

        // SOS pulse animation
        val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse)
        binding.btnSos.startAnimation(pulseAnim)

        checkPermissions()

        binding.btnSos.setOnClickListener { triggerSos() }
        binding.fabEmergency.setOnClickListener { triggerSos() }
        
        setupVoiceRecording()

        binding.cardFakeCall.setOnClickListener { startActivity(Intent(this, FakeCallSetupActivity::class.java)) }
        binding.cardFollowMe.setOnClickListener { startActivity(Intent(this, FollowMeActivity::class.java)) }
        binding.cardSafetyTips.setOnClickListener { startActivity(Intent(this, SafetyTipsActivity::class.java)) }
        binding.cardSelfDefense.setOnClickListener { startActivity(Intent(this, SelfDefenseActivity::class.java)) }
        binding.cardContacts.setOnClickListener { startActivity(Intent(this, ContactsActivity::class.java)) }
        binding.cardSettings.setOnClickListener {
            try {
                android.util.Log.d("NAV", "Opening Settings")
                startActivity(Intent(this, com.example.safesafar.ui.SettingsActivity::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        startSosService()
    }

    private var filePath: String? = null

    private fun setupVoiceRecording() {
        binding.btnMic.setOnTouchListener { _, event ->
            val prefs = getSharedPreferences("SafeSafarPrefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("voice_enabled", true)) {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    Toast.makeText(this, "Voice recording disabled in Settings", Toast.LENGTH_SHORT).show()
                }
                return@setOnTouchListener false
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    Toast.makeText(this, "Enable microphone permission", Toast.LENGTH_SHORT).show()
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        101
                    )
                }
                return@setOnTouchListener false
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startVoiceRecording()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopVoiceRecordingAndShare()
                    true
                }
                else -> false
            }
        }
    }

    private fun startVoiceRecording() {
        if (isRecording) return
        isRecording = true
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.requestAudioFocus(
                null,
                android.media.AudioManager.STREAM_MUSIC,
                android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )

            val file = File(cacheDir, "recording.mp4")
            filePath = file.absolutePath

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(filePath)
                prepare()
            }
            
            // Add small delay to fix device-specific timing/audio source busy issues
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isRecording) return@postDelayed // Button released too quickly
                
                try {
                    mediaRecorder?.start()
                    
                    // Timer and UI update ONLY start after recorder successfully starts
                    binding.btnMic.text = "RECORDING..."
                    binding.btnMic.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start()
                    binding.tvMicTimer.visibility = android.view.View.VISIBLE
                    secondsRecorded = 0
                    timerRunnable = object : Runnable {
                        override fun run() {
                            secondsRecorded++
                            val mins = secondsRecorded / 60
                            val secs = secondsRecorded % 60
                            binding.tvMicTimer.text = String.format("%02d:%02d", mins, secs)
                            
                            // Max duration limit (15 seconds) to prevent large files / WhatsApp errors
                            if (secondsRecorded >= 15) {
                                stopVoiceRecordingAndShare()
                                return
                            }
                            
                            timerHandler.postDelayed(this, 1000)
                        }
                    }
                    timerHandler.post(timerRunnable!!)
                    vibrate(50)
                } catch (e: Exception) {
                    android.util.Log.e("RECORD_ERROR", "start failed: ${e.message}")
                    isRecording = false
                    Toast.makeText(this@MainActivity, "Recording failed", Toast.LENGTH_SHORT).show()
                }
            }, 200)

        } catch (e: Exception) {
            android.util.Log.e("RECORD_ERROR", "prepare/audio focus error: ${e.message}")
            isRecording = false
            Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopVoiceRecordingAndShare() {
        if (!isRecording) return
        isRecording = false
        try {
            timerRunnable?.let { timerHandler.removeCallbacks(it) }
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
            binding.btnMic.text = "HOLD TO RECORD"
            binding.btnMic.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
            binding.tvMicTimer.visibility = android.view.View.GONE
            vibrate(50)
            
            audioFile = File(filePath ?: "")
            android.util.Log.e("FILE_CHECK", "Size: ${audioFile?.length()}")
            
            // Ensures file is fully written before sharing
            Handler(Looper.getMainLooper()).postDelayed({
                shareAudioFile()
            }, 500)
            
        } catch (e: Exception) {
            android.util.Log.e("STOP_ERROR", e.message.toString())
            binding.btnMic.text = "HOLD TO RECORD"
            binding.btnMic.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
        }
    }

    private fun shareAudioFile() {
        val file = File(cacheDir, "recording.mp4")
        if (!file.exists() || file.length() == 0L) {
            Toast.makeText(this, "Recording failed or empty file", Toast.LENGTH_SHORT).show()
            return
        }
        
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        startActivity(Intent.createChooser(shareIntent, "Send SOS Audio"))
        
        // Trigger fallback SMS (SOS link with context)
        val serviceIntent = Intent(this, SosService::class.java)
        serviceIntent.action = "ACTION_SEND_FALLBACK_SMS"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun vibrate(duration: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(duration)
        }
    }

    private fun startSosService() {
        val serviceIntent = Intent(this, SosService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun triggerSos() {
        val prefs = getSharedPreferences("SafeSafarPrefs", MODE_PRIVATE)
        if (!prefs.getBoolean("tap_enabled", true)) {
            Toast.makeText(this, "Tap SOS is disabled in Settings", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("SOS Triggered 🚨")
            .setMessage("Sending alerts to all contacts...")
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()

        val customMsg = binding.etCustomMessage.text.toString()
        val serviceIntent = Intent(this, SosService::class.java).apply {
            action = "ACTION_TRIGGER_SOS"
            putExtra("CUSTOM_MSG", customMsg)
            putExtra("FROM_BUTTON", true)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }
}

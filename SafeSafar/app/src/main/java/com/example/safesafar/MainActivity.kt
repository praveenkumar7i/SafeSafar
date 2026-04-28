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
            Manifest.permission.SEND_SMS
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

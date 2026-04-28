package com.example.safesafar.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.safesafar.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("SafeSafarPrefs", Context.MODE_PRIVATE)

        fun bindSwitch(id: Int, key: String, def: Boolean) {
            val sw = findViewById<SwitchMaterial?>(id)
            sw?.apply {
                isChecked = prefs.getBoolean(key, def)
                setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit().putBoolean(key, isChecked).apply()
                }
            }
        }

        bindSwitch(R.id.switchShake, "shake_enabled", true)
        bindSwitch(R.id.switchPower, "power_enabled", true)
        bindSwitch(R.id.switchTap, "tap_enabled", true)
        bindSwitch(R.id.switchAutoSms, "auto_sms_enabled", true)
        bindSwitch(R.id.switchFakeCall, "auto_fake_call_enabled", false)
        bindSwitch(R.id.switchSiren, "siren_enabled", false)
        bindSwitch(R.id.switchFlashlight, "flashlight_enabled", false)

        val pickAudioLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                prefs.edit().putString("custom_siren_uri", uri.toString()).apply()
                android.widget.Toast.makeText(this, "Custom siren selected", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        val btnChooseSiren = findViewById<MaterialButton>(R.id.btnChooseSiren)
        btnChooseSiren?.setOnClickListener {
            pickAudioLauncher.launch(arrayOf("audio/*"))
        }

        val btnOpenSettings = findViewById<android.widget.Button?>(R.id.btnOpenSettings)
        btnOpenSettings?.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            })
        }

        val btnBattery = findViewById<android.widget.Button?>(R.id.btnBattery)
        btnBattery?.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    })
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
        }
    }


}
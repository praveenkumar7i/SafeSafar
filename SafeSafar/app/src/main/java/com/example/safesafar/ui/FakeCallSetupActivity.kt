package com.example.safesafar.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.safesafar.R

class FakeCallSetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fake_call_setup)

        val spinner = findViewById<Spinner>(R.id.spinnerDelay)
        val delays = arrayOf("Immediate", "5 Seconds", "10 Seconds", "30 Seconds")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, delays)

        val etCallerName = findViewById<EditText>(R.id.etCallerName)
        val btnSchedule = findViewById<Button>(R.id.btnScheduleCall)

        btnSchedule.setOnClickListener {
            val callerName = etCallerName.text.toString().ifBlank { "Mom" }
            val delayStr = spinner.selectedItem.toString()
            val delayMs = when (delayStr) {
                "5 Seconds" -> 5000L
                "10 Seconds" -> 10000L
                "30 Seconds" -> 30000L
                else -> 0L
            }

            Toast.makeText(this, "Fake call scheduled in $delayStr", Toast.LENGTH_SHORT).show()
            finish()

            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, FakeCallActivity::class.java).apply {
                    putExtra("CALLER_NAME", callerName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }, delayMs)
        }
    }
}

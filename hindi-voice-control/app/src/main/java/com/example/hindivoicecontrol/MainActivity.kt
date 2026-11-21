package com.example.hindivoicecontrol

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var isServiceRunning = false

    private val statusText: TextView
        get() = findViewById(R.id.statusText)

    private val micButton: Button
        get() = findViewById(R.id.micButton)

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        micButton.setOnClickListener { toggleListening() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startListening()
        }
    }

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }

    fun startListening() {
        val intent = Intent(this, SpeechRecognitionService::class.java)
        intent.action = SpeechRecognitionService.ACTION_START
        startForegroundService(intent)
        isServiceRunning = true
        statusText.text = getString(R.string.status_listening)
    }

    fun stopListening() {
        stopService(Intent(this, SpeechRecognitionService::class.java))
        isServiceRunning = false
    }

    fun toggleListening() {
        if (isServiceRunning) stopListening() else startListening()
    }
}

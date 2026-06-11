package com.voiceai.assistant.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.voiceai.assistant.databinding.ActivitySettingsBinding
import com.voiceai.assistant.service.VoiceListenerService

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Settings"

        binding.switchBackground.setOnCheckedChangeListener { _, checked ->
            val intent = Intent(this, VoiceListenerService::class.java)
            if (checked) {
                startForegroundService(intent)
            } else {
                intent.action = VoiceListenerService.ACTION_STOP
                startService(intent)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

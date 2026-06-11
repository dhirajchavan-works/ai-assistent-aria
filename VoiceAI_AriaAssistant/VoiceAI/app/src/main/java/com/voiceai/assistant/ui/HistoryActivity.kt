package com.voiceai.assistant.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.voiceai.assistant.databinding.ActivityHistoryBinding

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Command History"
        binding.tvInfo.text = "Your command history stays private on this device.\nNo data is ever sent to any server."
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

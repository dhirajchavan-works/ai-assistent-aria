package com.voiceai.assistant.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.voiceai.assistant.R
import com.voiceai.assistant.databinding.ActivityMainBinding
import com.voiceai.assistant.engine.CommandProcessor
import com.voiceai.assistant.ui.adapter.ChatAdapter
import com.voiceai.assistant.utils.WakeWordDetector
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private lateinit var commandProcessor: CommandProcessor
    private var isListening = false
    private var isTtsReady = false

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val audioGranted = results[Manifest.permission.RECORD_AUDIO] == true
        if (audioGranted) {
            initSpeechRecognizer()
            showMessage("Permissions granted. Say 'Hey Aria' or tap the mic!")
        } else {
            showMessage("Microphone permission is required for voice commands.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        commandProcessor = CommandProcessor(this)
        tts = TextToSpeech(this, this)

        setupRecyclerView()
        setupClickListeners()
        requestPermissions()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupClickListeners() {
        // Mic button - tap to listen
        binding.fabMic.setOnClickListener {
            if (isListening) stopListening() else startListening()
        }

        // Send text command button
        binding.btnSend.setOnClickListener {
            val text = binding.etCommand.text.toString().trim()
            if (text.isNotEmpty()) {
                processCommand(text)
                binding.etCommand.setText("")
            }
        }

        // History button
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // Settings button
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.chatMessages.observe(this) { messages ->
            chatAdapter.submitList(messages.toList())
            if (messages.isNotEmpty()) {
                binding.rvChat.smoothScrollToPosition(messages.size - 1)
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isEmpty()) {
            initSpeechRecognizer()
        } else {
            permissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showMessage("On-device speech recognition not available on this device.")
            return
        }
        speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                runOnUiThread {
                    binding.tvStatus.text = "Listening…"
                    binding.fabMic.setImageResource(R.drawable.ic_mic_active)
                    binding.waveformView.visibility = View.VISIBLE
                    binding.waveformView.startAnimation()
                }
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                runOnUiThread { binding.waveformView.updateAmplitude(rmsdB) }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                runOnUiThread {
                    isListening = false
                    binding.tvStatus.text = "Processing…"
                    binding.waveformView.stopAnimation()
                }
            }

            override fun onError(error: Int) {
                runOnUiThread {
                    isListening = false
                    binding.waveformView.visibility = View.GONE
                    binding.fabMic.setImageResource(R.drawable.ic_mic)
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Try again!"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy, please wait."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied."
                        else -> "Error: $error. Tap mic to retry."
                    }
                    binding.tvStatus.text = msg
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                runOnUiThread {
                    isListening = false
                    binding.waveformView.visibility = View.GONE
                    binding.fabMic.setImageResource(R.drawable.ic_mic)
                    binding.tvStatus.text = "Tap mic or say a command"
                    if (!text.isNullOrBlank()) {
                        processCommand(text)
                    } else {
                        binding.tvStatus.text = "Didn't hear anything. Try again."
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    runOnUiThread { binding.etCommand.hint = partial }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        if (isListening) return
        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // KEY: Use on-device recognition only — no cloud
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        speechRecognizer.startListening(intent)
    }

    private fun stopListening() {
        isListening = false
        speechRecognizer.stopListening()
        binding.waveformView.stopAnimation()
        binding.tvStatus.text = "Tap mic or say a command"
        binding.fabMic.setImageResource(R.drawable.ic_mic)
    }

    fun processCommand(userInput: String) {
        // Add user message to chat
        viewModel.addUserMessage(userInput)

        // Process command locally (no network)
        val response = commandProcessor.process(userInput)

        // Add AI response to chat
        viewModel.addAIMessage(response.displayText)

        // Speak the response
        speak(response.spokenText ?: response.displayText)

        // Execute action (call, alarm, etc.)
        response.action?.invoke()
    }

    private fun speak(text: String) {
        if (isTtsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voiceai_${System.currentTimeMillis()}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.getDefault())
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (isTtsReady) {
                tts.setSpeechRate(0.95f)
                tts.setPitch(1.0f)
                speak("Hello! I'm Aria, your on-device assistant. Everything stays private on your phone.")
                binding.tvStatus.text = "Tap mic or say a command"
            }
        }
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognizer.isInitialized) speechRecognizer.destroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}

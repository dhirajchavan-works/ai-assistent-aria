package com.voiceai.assistant.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.voiceai.assistant.engine.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel : ViewModel() {

    private val _chatMessages = MutableLiveData<MutableList<ChatMessage>>(mutableListOf())
    val chatMessages: LiveData<MutableList<ChatMessage>> = _chatMessages

    fun addUserMessage(text: String) {
        val current = _chatMessages.value ?: mutableListOf()
        current.add(ChatMessage(
            text = text,
            isUser = true,
            timestamp = getCurrentTime()
        ))
        _chatMessages.value = current
    }

    fun addAIMessage(text: String) {
        val current = _chatMessages.value ?: mutableListOf()
        current.add(ChatMessage(
            text = text,
            isUser = false,
            timestamp = getCurrentTime()
        ))
        _chatMessages.value = current
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }

    fun clearHistory() {
        _chatMessages.value = mutableListOf()
    }
}

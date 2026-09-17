package com.example.domain.perception

import kotlinx.coroutines.flow.StateFlow

/**
 * Perception interface for voice input and speech recognition.
 * To be implemented in later phases (on-device SpeechRecognizer/fallback).
 */
interface SpeechProvider {
    /**
     * Whether voice recognition is available and permitted on this device.
     */
    val isAvailable: Boolean

    /**
     * Current speech recognition state.
     */
    val isListening: StateFlow<Boolean>

    /**
     * Starts listening for voice input and returns the transcribed text.
     */
    suspend fun startListening(): Result<String>

    /**
     * Cancels or stops active listening.
     */
    fun stopListening()
}

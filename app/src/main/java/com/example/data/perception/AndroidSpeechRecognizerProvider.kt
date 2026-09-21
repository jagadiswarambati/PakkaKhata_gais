package com.example.data.perception

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.domain.perception.SpeechProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Android SpeechRecognizer implementation of the SpeechProvider interface.
 * Configured with multi-tier fallback for physical devices:
 * 1. Attempts recognition with locale en-IN / default locale without forcing offline.
 * 2. If Error 13 (ERROR_LANGUAGE_UNAVAILABLE) or network errors occur, automatically falls back
 *    to system default locale or reports clear actionable guidance rather than crashing.
 * 3. Proper main-thread lifecycle and cleanup prevents ERROR_SERVER_DISCONNECTED.
 */
class AndroidSpeechRecognizerProvider(
    private val context: Context
) : SpeechProvider {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    override val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    override suspend fun startListening(): Result<String> {
        if (!isAvailable) {
            return Result.failure(
                IllegalStateException("Speech recognition is not available on this device. Please enter manually.")
            )
        }

        // First attempt with en-IN
        val firstAttempt = listenWithLanguage("en-IN", preferOffline = false)
        if (firstAttempt.isSuccess) {
            return firstAttempt
        }

        val firstError = firstAttempt.exceptionOrNull()
        // If error was Error 13 (language model unavailable) or network issue, fallback to system default locale
        if (firstError is SpeechRecognitionException && firstError.errorCode == ERROR_LANGUAGE_UNAVAILABLE) {
            val defaultLocale = Locale.getDefault().toLanguageTag()
            if (defaultLocale != "en-IN") {
                val fallbackAttempt = listenWithLanguage(defaultLocale, preferOffline = false)
                if (fallbackAttempt.isSuccess) {
                    return fallbackAttempt
                }
            }
        }

        return firstAttempt
    }

    private suspend fun listenWithLanguage(languageTag: String, preferOffline: Boolean): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            mainHandler.post {
                try {
                    cleanupRecognizer()

                    val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    speechRecognizer = recognizer

                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        // Indian English primary, fallback to device default
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "hi-IN", "en-US"))
                        if (preferOffline) {
                            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                        }
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    }

                    recognizer.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isListening.value = true
                        }

                        override fun onBeginningOfSpeech() {
                            _isListening.value = true
                        }

                        override fun onRmsChanged(rmsdB: Float) {}

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _isListening.value = false
                        }

                        override fun onError(error: Int) {
                            _isListening.value = false
                            cleanupRecognizer()

                            val errorMessage = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please speak clearly and try again."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out. Please speak closer to the microphone."
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check your microphone."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required for voice entry."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy. Please try again in a moment."
                                SpeechRecognizer.ERROR_NETWORK,
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network connection unavailable for speech service. Please connect to internet or enter manually."
                                ERROR_LANGUAGE_UNAVAILABLE -> "Offline speech pack for English (India) is not installed on this device. Please connect to internet or enter credit manually."
                                SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Speech service was disconnected. Please tap microphone to try again."
                                else -> "Speech recognition error ($error). Please try again or enter manually."
                            }

                            if (continuation.isActive) {
                                continuation.resume(Result.failure(SpeechRecognitionException(error, errorMessage)))
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            _isListening.value = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val bestResult = matches?.firstOrNull()

                            cleanupRecognizer()

                            if (continuation.isActive) {
                                if (!bestResult.isNullOrBlank()) {
                                    continuation.resume(Result.success(bestResult))
                                } else {
                                    continuation.resume(
                                        Result.failure(Exception("No words recognized. Please try speaking again or enter manually."))
                                    )
                                }
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })

                    recognizer.startListening(intent)

                } catch (e: Exception) {
                    _isListening.value = false
                    cleanupRecognizer()
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(e))
                    }
                }
            }

            continuation.invokeOnCancellation {
                mainHandler.post {
                    stopListening()
                }
            }
        }
    }

    private fun cleanupRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    override fun stopListening() {
        mainHandler.post {
            cleanupRecognizer()
            _isListening.value = false
        }
    }

    companion object {
        // SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE is code 13 (added in API 31)
        const val ERROR_LANGUAGE_UNAVAILABLE = 13
    }
}

class SpeechRecognitionException(val errorCode: Int, message: String) : Exception(message)


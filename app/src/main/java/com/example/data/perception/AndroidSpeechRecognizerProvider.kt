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
 * Configured with offline preference (RecognizerIntent.EXTRA_PREFER_OFFLINE = true)
 * for kirana store resilience without requiring cloud connectivity.
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

        return suspendCancellableCoroutine { continuation ->
            mainHandler.post {
                try {
                    // Clean up previous instance if needed
                    speechRecognizer?.destroy()

                    val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    speechRecognizer = recognizer

                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        // Indian English + Hindi shop counter speech
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-US"))
                        // Prefer on-device / offline recognition when models are downloaded
                        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
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
                            val errorMessage = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please speak clearly and try again."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out. Please try again."
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check your microphone."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required for voice entry."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Please try again."
                                SpeechRecognizer.ERROR_NETWORK,
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition offline model missing. Please use manual entry."
                                else -> "Speech recognition error ($error). Please try again or type manually."
                            }
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(Exception(errorMessage)))
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            _isListening.value = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val bestResult = matches?.firstOrNull()

                            if (continuation.isActive) {
                                if (!bestResult.isNullOrBlank()) {
                                    continuation.resume(Result.success(bestResult))
                                } else {
                                    continuation.resume(
                                        Result.failure(Exception("No words recognized. Please try speaking again."))
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

    override fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {}
            _isListening.value = false
        }
    }
}

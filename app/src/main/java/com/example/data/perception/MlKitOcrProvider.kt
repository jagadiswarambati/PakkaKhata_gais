package com.example.data.perception

import android.content.Context
import android.net.Uri
import com.example.domain.perception.OCRProvider
import com.example.domain.perception.OcrExtractionResult
import com.example.domain.perception.PaymentEvidenceParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * On-device optical character recognition provider using ML Kit Text Recognition.
 * Fully offline, no cloud APIs or internet connectivity required.
 */
class MlKitOcrProvider(
    private val context: Context
) : OCRProvider {

    override val isAvailable: Boolean = true

    override suspend fun processImage(imagePath: String): Result<OcrExtractionResult> {
        val file = File(imagePath)
        if (!file.exists() || file.length() == 0L) {
            return Result.failure(IllegalArgumentException("Image file does not exist or is empty: $imagePath"))
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromFilePath(context, Uri.fromFile(file))
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val raw = visionText.text
                        val parsed = PaymentEvidenceParser.parse(raw)
                        val result = OcrExtractionResult(
                            rawText = raw,
                            extractedAmountPaise = parsed.extractedAmount?.paise,
                            senderName = parsed.extractedSenderName,
                            utrNumber = parsed.utrNumber,
                            paymentApp = parsed.paymentApp,
                            confidence = if (parsed.extractedAmount != null) 0.9f else 0.3f
                        )
                        if (continuation.isActive) {
                            continuation.resume(Result.success(result))
                        }
                    }
                    .addOnFailureListener { exception ->
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(exception))
                        }
                    }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(Result.failure(e))
                }
            }
        }
    }
}

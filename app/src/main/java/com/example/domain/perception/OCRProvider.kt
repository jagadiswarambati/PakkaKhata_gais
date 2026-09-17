package com.example.domain.perception

/**
 * Result data extracted from visual payment evidence.
 */
data class OcrExtractionResult(
    val rawText: String,
    val extractedAmountPaise: Long? = null,
    val senderName: String? = null,
    val utrNumber: String? = null,
    val paymentApp: String? = null,
    val confidence: Float = 0f
)

/**
 * Perception interface for optical character recognition on payment evidence images.
 * To be implemented in later phases (on-device ML Kit / fallback).
 */
interface OCRProvider {
    /**
     * Whether on-device OCR is initialized and ready.
     */
    val isAvailable: Boolean

    /**
     * Processes an image file from the specified local path and returns OCR text & extracted entities.
     */
    suspend fun processImage(imagePath: String): Result<OcrExtractionResult>
}

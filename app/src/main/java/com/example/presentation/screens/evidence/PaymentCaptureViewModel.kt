package com.example.presentation.screens.evidence

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PakkaKhataDatabase
import com.example.data.perception.MlKitOcrProvider
import com.example.data.perception.PaymentImageManager
import com.example.data.repository.LedgerRepository
import com.example.data.repository.LedgerRepositoryImpl
import com.example.domain.model.Money
import com.example.domain.model.PaymentEvidence
import com.example.domain.perception.OCRProvider
import com.example.domain.perception.PaymentEvidenceParser
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CaptureUiPhase {
    CAMERA_PREVIEW,
    PROCESSING_IMAGE,
    PROCESSING_OCR,
    REVIEW,
    SAVED_READY,
    ERROR
}

data class ExtractedPaymentDetails(
    val amountRupees: String = "",
    val senderName: String = "",
    val utrNumber: String = "",
    val paymentApp: String = "",
    val rawOcrText: String = "",
    val isAmountMissing: Boolean = false
)

data class PaymentCaptureUiState(
    val phase: CaptureUiPhase = CaptureUiPhase.CAMERA_PREVIEW,
    val imagePath: String? = null,
    val extractedDetails: ExtractedPaymentDetails = ExtractedPaymentDetails(),
    val isTorchOn: Boolean = false,
    val errorMessage: String? = null,
    val savedEvidenceId: Long? = null
)

class PaymentCaptureViewModel(
    application: Application,
    private val ledgerRepository: LedgerRepository = LedgerRepositoryImpl(
        PakkaKhataDatabase.getDatabase(application)
    ),
    private val ocrProvider: OCRProvider = MlKitOcrProvider(application),
    private val imageManager: PaymentImageManager = PaymentImageManager(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PaymentCaptureUiState())
    val uiState: StateFlow<PaymentCaptureUiState> = _uiState.asStateFlow()

    fun getOutputMediaFile(): File {
        return imageManager.createCaptureOutputFile()
    }

    fun onImageCaptured(file: File) {
        _uiState.update {
            it.copy(
                phase = CaptureUiPhase.PROCESSING_OCR,
                imagePath = file.absolutePath,
                errorMessage = null
            )
        }
        processImage(file.absolutePath)
    }

    fun onGalleryImageSelected(uri: Uri) {
        _uiState.update {
            it.copy(
                phase = CaptureUiPhase.PROCESSING_IMAGE,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            val copyResult = imageManager.saveImageFromUri(uri)
            copyResult.onSuccess { file ->
                _uiState.update {
                    it.copy(
                        phase = CaptureUiPhase.PROCESSING_OCR,
                        imagePath = file.absolutePath
                    )
                }
                processImage(file.absolutePath)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        phase = CaptureUiPhase.ERROR,
                        errorMessage = "Could not load selected image: ${error.message}"
                    )
                }
            }
        }
    }

    fun toggleTorch() {
        _uiState.update { it.copy(isTorchOn = !it.isTorchOn) }
    }

    private fun processImage(imagePath: String) {
        viewModelScope.launch {
            val result = ocrProvider.processImage(imagePath)
            result.onSuccess { ocrResult ->
                if (ocrResult.rawText.isBlank()) {
                    _uiState.update {
                        it.copy(
                            phase = CaptureUiPhase.ERROR,
                            errorMessage = "Couldn't read payment details.",
                            extractedDetails = ExtractedPaymentDetails(
                                rawOcrText = "",
                                isAmountMissing = true
                            )
                        )
                    }
                } else {
                    val amountStr = ocrResult.extractedAmountPaise?.let { paise ->
                        if (paise % 100 == 0L) {
                            (paise / 100).toString()
                        } else {
                            String.format(java.util.Locale.US, "%.2f", paise / 100.0)
                        }
                    } ?: ""

                    _uiState.update {
                        it.copy(
                            phase = CaptureUiPhase.REVIEW,
                            extractedDetails = ExtractedPaymentDetails(
                                amountRupees = amountStr,
                                senderName = ocrResult.senderName ?: "",
                                utrNumber = ocrResult.utrNumber ?: "",
                                paymentApp = ocrResult.paymentApp ?: "",
                                rawOcrText = ocrResult.rawText,
                                isAmountMissing = amountStr.isBlank()
                            ),
                            errorMessage = if (amountStr.isBlank()) "Amount not detected. Please enter amount." else null
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        phase = CaptureUiPhase.ERROR,
                        errorMessage = "OCR processing failed: ${error.message}"
                    )
                }
            }
        }
    }

    fun updateAmount(amount: String) {
        _uiState.update {
            it.copy(
                extractedDetails = it.extractedDetails.copy(
                    amountRupees = amount,
                    isAmountMissing = amount.isBlank()
                ),
                errorMessage = null
            )
        }
    }

    fun updateSenderName(name: String) {
        _uiState.update {
            it.copy(extractedDetails = it.extractedDetails.copy(senderName = name))
        }
    }

    fun updateUtr(utr: String) {
        _uiState.update {
            it.copy(extractedDetails = it.extractedDetails.copy(utrNumber = utr))
        }
    }

    fun updatePaymentApp(app: String) {
        _uiState.update {
            it.copy(extractedDetails = it.extractedDetails.copy(paymentApp = app))
        }
    }

    fun startManualEntry() {
        _uiState.update {
            it.copy(
                phase = CaptureUiPhase.REVIEW,
                errorMessage = null,
                extractedDetails = it.extractedDetails.copy(
                    isAmountMissing = it.extractedDetails.amountRupees.isBlank()
                )
            )
        }
    }

    fun retake() {
        _uiState.update {
            PaymentCaptureUiState(phase = CaptureUiPhase.CAMERA_PREVIEW)
        }
    }

    fun savePaymentEvidence() {
        val currentState = _uiState.value
        val amountText = currentState.extractedDetails.amountRupees.trim()
        val amountDouble = amountText.toDoubleOrNull()

        if (amountDouble == null || amountDouble <= 0.0) {
            _uiState.update {
                it.copy(
                    errorMessage = "Please enter a valid payment amount.",
                    extractedDetails = it.extractedDetails.copy(isAmountMissing = true)
                )
            }
            return
        }

        val paise = (amountDouble * 100.0 + 0.5).toLong()
        val money = Money.fromPaise(paise)
        val imagePath = currentState.imagePath ?: ""

        val evidence = PaymentEvidence(
            imagePath = imagePath,
            extractedAmount = money,
            extractedSenderName = currentState.extractedDetails.senderName.trim().ifBlank { null },
            utrNumber = currentState.extractedDetails.utrNumber.trim().ifBlank { null },
            ocrRawText = currentState.extractedDetails.rawOcrText,
            paymentApp = currentState.extractedDetails.paymentApp.trim().ifBlank { null },
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            try {
                val savedId = ledgerRepository.evidenceRepo.saveEvidence(evidence)
                _uiState.update {
                    it.copy(
                        phase = CaptureUiPhase.SAVED_READY,
                        savedEvidenceId = savedId,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Failed to save payment evidence: ${e.message}"
                    )
                }
            }
        }
    }
}

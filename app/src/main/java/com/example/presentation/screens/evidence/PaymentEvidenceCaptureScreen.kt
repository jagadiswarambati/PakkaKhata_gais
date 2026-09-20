package com.example.presentation.screens.evidence

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderMediumDark
import com.example.ui.theme.BorderSubtleDark
import com.example.ui.theme.IQOOLime
import com.example.ui.theme.IQOOLimeContainer
import com.example.ui.theme.IQOOOnLime
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHigherDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun PaymentEvidenceCaptureScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToReconcile: ((Long) -> Unit)? = null,
    viewModel: PaymentCaptureViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Modern zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onGalleryImageSelected(uri)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("payment_capture_screen")
    ) {
        when (uiState.phase) {
            CaptureUiPhase.CAMERA_PREVIEW -> {
                if (hasCameraPermission) {
                    PaymentCameraPreview(
                        isTorchOn = uiState.isTorchOn,
                        onToggleTorch = { viewModel.toggleTorch() },
                        onCapture = { file -> viewModel.onImageCaptured(file) },
                        onOpenGallery = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onManualEntry = { viewModel.startManualEntry() },
                        onNavigateBack = onNavigateBack,
                        outputFileProvider = { viewModel.getOutputMediaFile() }
                    )
                } else {
                    // Permission not granted fallback UI
                    CameraPermissionFallback(
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        onOpenGallery = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onManualEntry = { viewModel.startManualEntry() },
                        onNavigateBack = onNavigateBack
                    )
                }
            }

            CaptureUiPhase.PROCESSING_IMAGE,
            CaptureUiPhase.PROCESSING_OCR -> {
                ProcessingOcrOverlay(
                    message = if (uiState.phase == CaptureUiPhase.PROCESSING_IMAGE) {
                        "Preparing payment screenshot..."
                    } else {
                        "Scanning payment evidence on-device..."
                    }
                )
            }

            CaptureUiPhase.REVIEW,
            CaptureUiPhase.ERROR -> {
                PaymentEvidenceReviewScreen(
                    imagePath = uiState.imagePath,
                    extractedDetails = uiState.extractedDetails,
                    errorMessage = uiState.errorMessage,
                    isErrorPhase = uiState.phase == CaptureUiPhase.ERROR,
                    onAmountChange = { viewModel.updateAmount(it) },
                    onSenderChange = { viewModel.updateSenderName(it) },
                    onUtrChange = { viewModel.updateUtr(it) },
                    onPaymentAppChange = { viewModel.updatePaymentApp(it) },
                    onConfirmAndSave = { viewModel.savePaymentEvidence() },
                    onRetake = { viewModel.retake() },
                    onOpenGallery = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onManualEntry = { viewModel.startManualEntry() },
                    onNavigateBack = onNavigateBack
                )
            }

            CaptureUiPhase.SAVED_READY -> {
                PaymentSavedConfirmation(
                    extractedDetails = uiState.extractedDetails,
                    evidenceId = uiState.savedEvidenceId,
                    onNavigateBack = onNavigateBack,
                    onCaptureAnother = { viewModel.retake() },
                    onReconcile = onNavigateToReconcile
                )
            }
        }
    }
}

@Composable
private fun ProcessingOcrOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE070908))
            .testTag("ocr_processing_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                color = IQOOLime,
                strokeWidth = 4.dp,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "100% Offline On-Device OCR Engine",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun CameraPermissionFallback(
    onRequestPermission: () -> Unit,
    onOpenGallery: () -> Unit,
    onManualEntry: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp)
            .testTag("camera_permission_fallback")
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(SurfaceHigherDark, CircleShape)
                    .border(1.dp, BorderMediumDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = IQOOLime,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Camera Access Needed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To photograph payment confirmation screens, PakkaKhata requires camera access. You can also pick a screenshot from your gallery.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = IQOOLime,
                    contentColor = IQOOOnLime
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("grant_camera_permission_button")
            ) {
                Text("Grant Camera Permission", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onOpenGallery,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderMediumDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("fallback_choose_gallery_button")
            ) {
                Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = IQOOLime)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Choose Screenshot from Gallery")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onManualEntry,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderMediumDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("fallback_manual_entry_button")
            ) {
                Icon(imageVector = Icons.Default.Keyboard, contentDescription = null, tint = IQOOLime)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enter Payment Details Manually")
            }
        }
    }
}

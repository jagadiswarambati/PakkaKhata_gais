package com.example.presentation.screens.evidence

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.PakkaTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentEvidenceReviewScreen(
    imagePath: String?,
    extractedDetails: ExtractedPaymentDetails,
    errorMessage: String?,
    isErrorPhase: Boolean,
    onAmountChange: (String) -> Unit,
    onSenderChange: (String) -> Unit,
    onUtrChange: (String) -> Unit,
    onPaymentAppChange: (String) -> Unit,
    onConfirmAndSave: () -> Unit,
    onRetake: () -> Unit,
    onOpenGallery: () -> Unit,
    onManualEntry: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PakkaTheme.colors
    var isRawOcrExpanded by remember { mutableStateOf(false) }

    val popularApps = listOf("Google Pay", "PhonePe", "Paytm", "BHIM", "CRED")

    Scaffold(
        containerColor = colors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isErrorPhase) "OCR Unsuccessful" else "Review Payment Evidence",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("review_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colors.surfaceElevated
                )
            )
        },
        modifier = modifier.testTag("payment_review_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image Preview Thumbnail
            if (!imagePath.isNullOrBlank()) {
                val imageFile = remember(imagePath) { File(imagePath) }
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceCardElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderMedium),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("payment_image_preview")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = imageFile,
                            contentDescription = "Captured Payment Evidence",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            shape = RoundedCornerShape(topStart = 10.dp),
                            color = colors.surface.copy(alpha = 0.85f),
                            modifier = Modifier.align(Alignment.BottomEnd)
                        ) {
                            Text(
                                text = "On-Device Capture",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Error / Warning Banner if OCR was unsuccessful or details need attention
            if (isErrorPhase || errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = colors.openRedContainer
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.openRed.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("ocr_warning_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = colors.openRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = errorMessage ?: "Couldn't read payment details.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.openRed
                            )
                            if (isErrorPhase) {
                                Text(
                                    text = "You can enter payment details manually or retry.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // If in Error Phase, show primary alternative action buttons
            if (isErrorPhase) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onManualEntry,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_enter_manually"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.limePrimary,
                            contentColor = colors.onLimePrimary
                        )
                    ) {
                        Text("Enter Manually", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_retake_error"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderMedium)
                    ) {
                        Text("Retake Photo")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenGallery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_choose_gallery_error"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderMedium)
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = colors.limePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose Another Image")
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Editable Extracted Fields Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.surfaceCardElevated
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderMedium),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.limePrimary,
                        unfocusedBorderColor = colors.borderMedium,
                        focusedLabelColor = colors.limePrimary,
                        unfocusedLabelColor = colors.textSecondary,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.limePrimary
                    )

                    // Amount Field (Crucial)
                    Text(
                        text = "PAYMENT AMOUNT *",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = extractedDetails.amountRupees,
                        onValueChange = onAmountChange,
                        leadingIcon = {
                            Text(
                                text = "₹",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.limePrimary
                            )
                        },
                        placeholder = { Text("e.g. 500", color = colors.textTertiary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = extractedDetails.isAmountMissing,
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_amount")
                    )
                    if (extractedDetails.isAmountMissing) {
                        Text(
                            text = "Amount is required to save payment evidence",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.openRed,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sender / Paid By Field
                    Text(
                        text = "PAID BY / SENDER NAME",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = extractedDetails.senderName,
                        onValueChange = onSenderChange,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = colors.limePrimary
                            )
                        },
                        placeholder = { Text("Sender Name (or Not detected)", color = colors.textTertiary) },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_sender")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // UTR / Transaction ID
                    Text(
                        text = "UPI REFERENCE / UTR NUMBER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = extractedDetails.utrNumber,
                        onValueChange = onUtrChange,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = colors.limePrimary
                            )
                        },
                        placeholder = { Text("12-digit UTR / Ref (or Not detected)", color = colors.textTertiary) },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_utr")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Payment Application Detected
                    Text(
                        text = "PAYMENT APP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        popularApps.forEach { appName ->
                            val isSelected = extractedDetails.paymentApp.equals(appName, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) onPaymentAppChange("") else onPaymentAppChange(appName)
                                },
                                label = { Text(appName, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.limeContainer,
                                    selectedLabelColor = if (colors.isDark) colors.limePrimary else colors.onLimeContainer,
                                    containerColor = colors.surfaceElevated,
                                    labelColor = colors.textSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) colors.limePrimary else colors.borderMedium
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expandable Raw OCR Text Section
            if (extractedDetails.rawOcrText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isRawOcrExpanded = !isRawOcrExpanded }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Raw OCR Text",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary
                            )
                            Icon(
                                imageVector = if (isRawOcrExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = colors.textSecondary
                            )
                        }
                        AnimatedVisibility(visible = isRawOcrExpanded) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Text(
                                    text = extractedDetails.rawOcrText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textTertiary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Primary Action: Confirm & Save Evidence
            Button(
                onClick = onConfirmAndSave,
                enabled = extractedDetails.amountRupees.isNotBlank() && (extractedDetails.amountRupees.toDoubleOrNull() ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.limePrimary,
                    contentColor = colors.onLimePrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_save_evidence_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirm & Save Evidence",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Actions: Retake or Choose Another Image
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRetake,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderMedium),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("retake_photo_button")
                ) {
                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, tint = colors.limePrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retake")
                }
                OutlinedButton(
                    onClick = onOpenGallery,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderMedium),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("choose_another_image_button")
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = colors.limePrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gallery")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

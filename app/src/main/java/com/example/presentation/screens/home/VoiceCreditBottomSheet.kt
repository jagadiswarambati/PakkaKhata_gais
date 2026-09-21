package com.example.presentation.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PakkaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCreditBottomSheet(
    voiceState: VoiceCreditUiState,
    onConfirm: () -> Unit,
    onRetryVoice: () -> Unit,
    onDismiss: () -> Unit,
    onStartManual: () -> Unit,
    onToggleEdit: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    if (voiceState.state == VoiceEntryState.IDLE) return

    val colors = PakkaTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.testTag("voice_credit_bottom_sheet"),
        containerColor = colors.surfaceCardElevated,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.borderMedium)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (voiceState.state) {
                VoiceEntryState.LISTENING -> {
                    ListeningView(
                        onCancel = onDismiss,
                        onManual = onStartManual
                    )
                }

                VoiceEntryState.PROCESSING -> {
                    ProcessingView()
                }

                VoiceEntryState.READY_FOR_CONFIRMATION -> {
                    ConfirmationView(
                        voiceState = voiceState,
                        onConfirm = onConfirm,
                        onToggleEdit = onToggleEdit,
                        onNameChange = onNameChange,
                        onAmountChange = onAmountChange,
                        onNoteChange = onNoteChange,
                        onCancel = onDismiss
                    )
                }

                VoiceEntryState.SAVING -> {
                    SavingView()
                }

                VoiceEntryState.SUCCESS -> {
                    SuccessView(
                        summary = voiceState.successSummary,
                        onDone = onDismiss
                    )
                }

                VoiceEntryState.ERROR -> {
                    ErrorView(
                        errorMessage = voiceState.errorMessage ?: "Could not recognize audio",
                        rawTranscript = voiceState.rawTranscript,
                        onRetry = onRetryVoice,
                        onManual = onStartManual,
                        onCancel = onDismiss
                    )
                }

                VoiceEntryState.IDLE -> {}
            }
        }
    }
}

@Composable
private fun ListeningView(
    onCancel: () -> Unit,
    onManual: () -> Unit
) {
    val colors = PakkaTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Animated Mic Indicator with Lime Glow
    Box(
        modifier = Modifier
            .size(108.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(colors.limePrimary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.limePrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Active Microphone",
                tint = colors.onLimePrimary,
                modifier = Modifier.size(36.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Listening for Credit...",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary
    )

    Text(
        text = "Speak customer name and amount\ne.g., \"Ramesh 500 udhar\" or \"Suresh 250 doodh\"",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.textSecondary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OutlinedButton(
            onClick = onManual,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderMedium),
            modifier = Modifier.testTag("type_manually_button")
        ) {
            Icon(Icons.Default.Keyboard, contentDescription = null, tint = colors.limePrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Type Manually")
        }

        TextButton(
            onClick = onCancel,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("cancel_listening_button")
        ) {
            Text("Cancel", color = colors.textSecondary)
        }
    }
}

@Composable
private fun ProcessingView() {
    val colors = PakkaTheme.colors
    Spacer(modifier = Modifier.height(16.dp))
    CircularProgressIndicator(
        modifier = Modifier.size(48.dp),
        color = colors.limePrimary,
        strokeWidth = 4.dp
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Parsing Voice Note...",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary
    )
    Text(
        text = "Extracting customer name & credit amount",
        style = MaterialTheme.typography.bodySmall,
        color = colors.textSecondary
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun ConfirmationView(
    voiceState: VoiceCreditUiState,
    onConfirm: () -> Unit,
    onToggleEdit: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onCancel: () -> Unit
) {
    val colors = PakkaTheme.colors

    Text(
        text = "Credit Entry",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = colors.textPrimary
    )

    if (voiceState.isEditing) {
        // Editable Form
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.limePrimary,
                unfocusedBorderColor = colors.borderMedium,
                focusedLabelColor = colors.limePrimary,
                unfocusedLabelColor = colors.textSecondary,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                cursorColor = colors.limePrimary
            )

            OutlinedTextField(
                value = voiceState.customerName,
                onValueChange = onNameChange,
                label = { Text("Customer Name") },
                singleLine = true,
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_customer_name_field"),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            OutlinedTextField(
                value = voiceState.amountRupees,
                onValueChange = onAmountChange,
                label = { Text("Amount (₹)") },
                prefix = { Text("₹ ", color = colors.limePrimary) },
                singleLine = true,
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_amount_field"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = voiceState.optionalNote,
                onValueChange = onNoteChange,
                label = { Text("Note (Optional, e.g. doodh, ration)") },
                singleLine = true,
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_note_field")
            )

            Button(
                onClick = { onToggleEdit(false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surfaceElevated,
                    contentColor = colors.textPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("done_editing_button")
            ) {
                Text("Done Editing", fontWeight = FontWeight.SemiBold)
            }
        }
    } else {
        // Shop-counter Friendly Display Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("confirmation_card"),
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            ),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = voiceState.customerName.ifBlank { "Unknown Customer" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Text(
                    text = "₹${voiceState.amountRupees.ifBlank { "0" }}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.limePrimary
                )

                if (voiceState.optionalNote.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
                    ) {
                        Text(
                            text = "Note: ${voiceState.optionalNote}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                if (voiceState.rawTranscript.isNotBlank() && voiceState.rawTranscript != "Manual Entry") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${voiceState.rawTranscript}\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = colors.textTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (voiceState.errorMessage != null) {
            Text(
                text = voiceState.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = colors.openRed,
                textAlign = TextAlign.Center
            )
        }

        // Action Buttons
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("confirm_credit_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.limePrimary,
                contentColor = colors.onLimePrimary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Confirm Credit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = { onToggleEdit(true) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderMedium),
                modifier = Modifier.testTag("edit_credit_button")
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = colors.limePrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit")
            }

            TextButton(
                onClick = onCancel,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("cancel_credit_button")
            ) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun SavingView() {
    val colors = PakkaTheme.colors
    Spacer(modifier = Modifier.height(16.dp))
    CircularProgressIndicator(
        modifier = Modifier.size(48.dp),
        color = colors.limePrimary,
        strokeWidth = 4.dp
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Saving to Ledger...",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun SuccessView(
    summary: CreditSuccessSummary?,
    onDone: () -> Unit
) {
    val colors = PakkaTheme.colors
    Spacer(modifier = Modifier.height(8.dp))

    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = "Success",
        tint = colors.settledGreen,
        modifier = Modifier.size(64.dp)
    )

    Text(
        text = "Credit Added",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.ExtraBold,
        color = colors.textPrimary
    )

    if (summary != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            ),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = summary.customerName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "${summary.amount.formatRupees()} added",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
                Text(
                    text = "${summary.totalOutstanding.formatRupees()} total outstanding",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.limePrimary
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Button(
        onClick = onDone,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("dismiss_success_button"),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.limePrimary,
            contentColor = colors.onLimePrimary
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text("Done", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ErrorView(
    errorMessage: String,
    rawTranscript: String,
    onRetry: () -> Unit,
    onManual: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = PakkaTheme.colors
    Icon(
        imageVector = Icons.Default.ErrorOutline,
        contentDescription = "Error",
        tint = colors.openRed,
        modifier = Modifier.size(56.dp)
    )

    Text(
        text = "Voice Entry Issue",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = colors.openRed
    )

    Text(
        text = errorMessage,
        style = MaterialTheme.typography.bodyMedium,
        color = colors.textSecondary,
        textAlign = TextAlign.Center
    )

    if (rawTranscript.isNotBlank() && rawTranscript != "Manual Entry") {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
        ) {
            Text(
                text = "Heard: \"$rawTranscript\"",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.limePrimary,
                contentColor = colors.onLimePrimary
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("retry_voice_button")
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.onLimePrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Speaking Again", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onManual,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderMedium),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_fallback_button")
        ) {
            Icon(Icons.Default.Keyboard, contentDescription = null, tint = colors.limePrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Enter Details Manually")
        }

        TextButton(
            onClick = onCancel,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cancel_error_button")
        ) {
            Text("Cancel", color = colors.textSecondary)
        }
    }
}

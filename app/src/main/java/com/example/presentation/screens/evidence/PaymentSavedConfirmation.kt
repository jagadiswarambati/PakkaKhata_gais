package com.example.presentation.screens.evidence

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderMediumDark
import com.example.ui.theme.IQOOLime
import com.example.ui.theme.IQOOLimeContainer
import com.example.ui.theme.IQOOOnLime
import com.example.ui.theme.PartialAmber
import com.example.ui.theme.PartialAmberContainer
import com.example.ui.theme.SettledGreen
import com.example.ui.theme.SettledGreenContainer
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.SurfaceHigherDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun PaymentSavedConfirmation(
    extractedDetails: ExtractedPaymentDetails,
    evidenceId: Long?,
    onNavigateBack: () -> Unit,
    onCaptureAnother: () -> Unit,
    modifier: Modifier = Modifier,
    onReconcile: ((Long) -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp)
            .testTag("payment_saved_confirmation"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Success Badge Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(SettledGreenContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = SettledGreen,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Payment Evidence Saved",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PartialAmberContainer,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PartialAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "READY FOR RECONCILIATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PartialAmber,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Summary Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceCardElevated
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderMediumDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Amount Received", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(
                            text = "₹${extractedDetails.amountRupees}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = IQOOLime
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Paid By", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(
                            text = extractedDetails.senderName.ifBlank { "Not detected" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("UPI Ref / UTR", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(
                            text = extractedDetails.utrNumber.ifBlank { "Not detected" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }

                    if (extractedDetails.paymentApp.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Payment App", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text(
                                text = extractedDetails.paymentApp,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Primary action: Reconcile Now if evidenceId available
            if (evidenceId != null && onReconcile != null) {
                Button(
                    onClick = { onReconcile(evidenceId) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IQOOLime,
                        contentColor = IQOOOnLime
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_reconcile_now")
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Review Match & Settle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Action Buttons
            OutlinedButton(
                onClick = onNavigateBack,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderMediumDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_back_to_ledger")
            ) {
                Text(
                    text = "Back to Ledger",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onCaptureAnother,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderMediumDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_capture_another")
            ) {
                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, tint = IQOOLime)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Capture Another Payment")
            }
        }
    }
}

package com.example.presentation.screens.reconciliation

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.SettlementOutcome
import com.example.ui.theme.PakkaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementResultScreen(
    reconciliationId: Long,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettlementResultViewModel = viewModel()
) {
    val colors = PakkaTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(reconciliationId) {
        viewModel.load(reconciliationId)
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settlement Result",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colors.surfaceElevated
                )
            )
        },
        modifier = modifier.testTag("settlement_result_screen")
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = colors.limePrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading settlement record...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                }

                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = colors.openRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = uiState.errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.openRed
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onDone,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.limePrimary,
                                contentColor = colors.onLimePrimary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Return to Ledger", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                else -> {
                    val reconciliation = uiState.reconciliation
                    val obligation = uiState.obligation
                    val customer = uiState.customer
                    val evidence = uiState.evidence

                    if (reconciliation != null && obligation != null && customer != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Big status indicator
                            val (badgeBg, badgeFg, statusTitle) = when (reconciliation.matchType) {
                                SettlementOutcome.FULLY_SETTLED -> Triple(
                                    colors.settledGreenContainer,
                                    colors.settledGreen,
                                    "FULLY SETTLED"
                                )
                                SettlementOutcome.PARTIALLY_SETTLED -> Triple(
                                    colors.partialAmberContainer,
                                    colors.partialAmber,
                                    "PARTIALLY SETTLED"
                                )
                                SettlementOutcome.OVERPAID -> Triple(
                                    colors.overpaidBlueContainer,
                                    colors.overpaidBlue,
                                    "OVERPAID"
                                )
                                else -> Triple(
                                    colors.surfaceHigher,
                                    colors.textSecondary,
                                    reconciliation.matchType.name
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = badgeBg,
                                modifier = Modifier.size(76.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (reconciliation.matchType == SettlementOutcome.FULLY_SETTLED) {
                                             Icons.Default.CheckCircle
                                        } else Icons.Default.Check,
                                        contentDescription = null,
                                        tint = badgeFg,
                                        modifier = Modifier.size(42.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Dominant settlement amounts display
                            Text(
                                text = "${reconciliation.settledAmount.formatRupees()} received",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = colors.settledGreen
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${obligation.remainingAmount.formatRupees()} remaining",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (obligation.remainingAmount.isZero) colors.settledGreen else colors.openRed
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Status Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = badgeBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, badgeFg.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = statusTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = badgeFg,
                                    letterSpacing = 1.5.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Detailed Summary Card
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = colors.surfaceCardElevated
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderMedium),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = "TRANSACTION SUMMARY",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.textSecondary,
                                        letterSpacing = 1.sp
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    SummaryRow(label = "Customer", value = customer.name)
                                    SummaryRow(
                                        label = "Customer Balance",
                                        value = customer.currentBalance.formatRupees()
                                    )
                                    SummaryRow(
                                        label = "Original Credit",
                                        value = obligation.originalAmount.formatRupees()
                                    )
                                    SummaryRow(
                                        label = "Settled In Payment",
                                        value = reconciliation.settledAmount.formatRupees(),
                                        highlightColor = colors.settledGreen
                                    )
                                    SummaryRow(
                                        label = "Remaining Due",
                                        value = obligation.remainingAmount.formatRupees(),
                                        highlightColor = if (obligation.remainingAmount.isZero) colors.settledGreen else colors.openRed
                                    )

                                    if (evidence?.utrNumber != null) {
                                        SummaryRow(label = "Payment Reference / UTR", value = evidence.utrNumber)
                                    }

                                    if (evidence?.paymentApp != null) {
                                        SummaryRow(label = "Payment App", value = evidence.paymentApp)
                                    }

                                    SummaryRow(
                                        label = "Settled At",
                                        value = reconciliation.reconciledAt.formatDateTime()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Office Kit Shared Clipboard action
                            val context = androidx.compose.ui.platform.LocalContext.current
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    val summary = com.example.domain.officebridge.OfficeBridgeService.formatSettlementClipboardSummary(
                                        customerName = customer.name,
                                        originalCredit = obligation.originalAmount,
                                        paymentReceived = reconciliation.settledAmount,
                                        remainingDue = obligation.remainingAmount,
                                        status = reconciliation.matchType,
                                        utrNumber = evidence?.utrNumber,
                                        paymentApp = evidence?.paymentApp,
                                        timestamp = java.util.Date(reconciliation.reconciledAt)
                                    )
                                    val success = com.example.domain.officebridge.OfficeBridgeService.copyToClipboard(
                                        context = context,
                                        label = "PakkaKhata Settlement",
                                        text = summary
                                    )
                                    if (success) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Copied for Office Kit Shared Clipboard",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.limePrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_copy_office_kit_summary")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = colors.limePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Copy for Office Kit (Shared Clipboard)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.limePrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Done action button
                            Button(
                                onClick = onDone,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.limePrimary,
                                    contentColor = colors.onLimePrimary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("btn_settlement_done")
                            ) {
                                Text(
                                    text = "Done",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    highlightColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val colors = PakkaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = highlightColor ?: colors.textPrimary
        )
    }
}

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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.example.domain.model.ObligationStatus
import com.example.domain.model.SettlementOutcome
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementResultScreen(
    reconciliationId: Long,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettlementResultViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(reconciliationId) {
        viewModel.load(reconciliationId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settlement Result",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                        CircularProgressIndicator(color = EmeraldPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading settlement record...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = uiState.errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onDone) {
                            Text("Return to Ledger")
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
                            Spacer(modifier = Modifier.height(8.dp))

                            // Big status indicator
                            val (badgeBg, badgeFg, statusTitle) = when (reconciliation.matchType) {
                                SettlementOutcome.FULLY_SETTLED -> Triple(
                                    Color(0xFFDEF7EC),
                                    Color(0xFF03543F),
                                    "FULLY SETTLED"
                                )
                                SettlementOutcome.PARTIALLY_SETTLED -> Triple(
                                    Color(0xFFFEF08A),
                                    Color(0xFF713F12),
                                    "PARTIALLY SETTLED"
                                )
                                SettlementOutcome.OVERPAID -> Triple(
                                    Color(0xFFE1EFFE),
                                    Color(0xFF1E429F),
                                    "OVERPAID"
                                )
                                else -> Triple(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                    reconciliation.matchType.name
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = badgeBg,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (reconciliation.matchType == SettlementOutcome.FULLY_SETTLED) {
                                            Icons.Default.CheckCircle
                                        } else Icons.Default.Check,
                                        contentDescription = null,
                                        tint = badgeFg,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = statusTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = badgeFg
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${reconciliation.settledAmount.formatRupees()} received",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )

                            Text(
                                text = "${obligation.remainingAmount.formatRupees()} remaining balance",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Detailed Summary Card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        text = "Transaction Summary",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
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
                                        highlightColor = EmeraldPrimary
                                    )
                                    SummaryRow(
                                        label = "Remaining Due",
                                        value = obligation.remainingAmount.formatRupees(),
                                        highlightColor = if (obligation.remainingAmount.isZero) EmeraldPrimary else Color(0xFFE02424)
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

                            Spacer(modifier = Modifier.height(32.dp))

                            // Done action button
                            Button(
                                onClick = onDone,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary
                                ),
                                shape = RoundedCornerShape(14.dp),
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

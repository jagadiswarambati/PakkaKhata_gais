package com.example.presentation.screens.customer

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.ObligationStatus
import com.example.presentation.util.DateTimeFormatter
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderAccentDark
import com.example.ui.theme.BorderMediumDark
import com.example.ui.theme.BorderSubtleDark
import com.example.ui.theme.IQOOLime
import com.example.ui.theme.IQOOLimeContainer
import com.example.ui.theme.IQOOOnLime
import com.example.ui.theme.OpenRed
import com.example.ui.theme.OpenRedContainer
import com.example.ui.theme.OverpaidBlue
import com.example.ui.theme.PartialAmber
import com.example.ui.theme.PartialAmberContainer
import com.example.ui.theme.SettledGreen
import com.example.ui.theme.SettledGreenContainer
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.SurfaceHigherDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: Long,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onNavigateToMatchReview: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: CustomerDetailViewModel = viewModel(
        factory = CustomerDetailViewModel.provideFactory(
            context.applicationContext as Application,
            customerId
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("customer_detail_screen"),
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.customer?.name ?: "Customer Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Account Ledger & Audit History",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("customer_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceElevatedDark
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = IQOOLime)
            }
        } else if (uiState.customer == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "Customer record not found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Customer Hero Balance Card
                item {
                    CustomerHeroCard(uiState = uiState)
                }

                // Tabs: Credit Obligations vs Payment History
                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = SurfaceHigherDark,
                        contentColor = IQOOLime,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = IQOOLime,
                                    height = 3.dp
                                )
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, BorderSubtleDark, RoundedCornerShape(16.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    text = "Credit (${uiState.obligations.size})",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 0) IQOOLime else TextSecondary,
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_credit_obligations")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    text = "Payments (${uiState.paymentHistory.size})",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 1) IQOOLime else TextSecondary,
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_payment_history")
                        )
                    }
                }

                if (selectedTab == 0) {
                    // Credit Obligations List
                    if (uiState.obligations.isEmpty()) {
                        item {
                            EmptySectionCard(
                                title = "No credit obligations",
                                message = "No credit has been recorded for this customer."
                            )
                        }
                    } else {
                        items(uiState.obligations, key = { it.obligation.id }) { item ->
                            CustomerObligationCard(item)
                        }
                    }
                } else {
                    // Payment History List
                    if (uiState.paymentHistory.isEmpty()) {
                        item {
                            EmptySectionCard(
                                title = "No payments recorded yet",
                                message = "Incoming payment evidence matched to this customer will appear here."
                            )
                        }
                    } else {
                        items(uiState.paymentHistory, key = { it.reconciliation.id }) { item ->
                            CustomerPaymentHistoryCard(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerHeroCard(uiState: CustomerDetailUiState) {
    val isDue = uiState.currentBalance.isPositive
    val totalPaise = maxOf(1L, uiState.totalCredit.paise)
    val progressFraction = (uiState.totalReceived.paise.toFloat() / totalPaise.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceCardElevated
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMediumDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceHigherDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (uiState.customer?.name ?: "C").take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = IQOOLime
                        )
                    }
                    Column {
                        Text(
                            text = uiState.customer?.name.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Customer Account #${uiState.customer?.id ?: 0}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDue) OpenRedContainer else SettledGreenContainer
                ) {
                    Text(
                        text = if (isDue) "DUE PENDING" else "ALL CLEAR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDue) OpenRed else SettledGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp
                    )
                }
            }

            // Financial Balance Banner (Prominent & High Contrast)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderSubtleDark, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "OUTSTANDING BALANCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.currentBalance.formatRupees(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDue) OpenRed else SettledGreen,
                        letterSpacing = (-0.5).sp,
                        fontSize = 34.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = SettledGreen,
                        trackColor = SurfaceHigherDark,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            // 3-Metric Financial Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "Total Credit",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = uiState.totalCredit.formatRupees(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Total Received",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = uiState.totalReceived.formatRupees(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SettledGreen
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = uiState.currentBalance.formatRupees(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDue) OpenRed else SettledGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerObligationCard(item: ObligationDetailUiModel) {
    val ob = item.obligation
    val receivedPaise = maxOf(0L, ob.originalAmount.paise - ob.remainingAmount.paise)
    val receivedMoney = com.example.domain.model.Money.fromPaise(receivedPaise)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_obligation_${ob.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceCardElevated
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtleDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = IQOOLime,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = ob.originalAmount.formatRupees(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (ob.status) {
                        ObligationStatus.OPEN -> OpenRedContainer
                        ObligationStatus.PARTIALLY_SETTLED -> PartialAmberContainer
                        ObligationStatus.FULLY_SETTLED -> SettledGreenContainer
                        ObligationStatus.OVERPAID -> SurfaceHigherDark
                    }
                ) {
                    Text(
                        text = when (ob.status) {
                            ObligationStatus.OPEN -> "OPEN"
                            ObligationStatus.PARTIALLY_SETTLED -> "PARTIAL"
                            ObligationStatus.FULLY_SETTLED -> "SETTLED"
                            ObligationStatus.OVERPAID -> "OVERPAID"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (ob.status) {
                            ObligationStatus.OPEN -> OpenRed
                            ObligationStatus.PARTIALLY_SETTLED -> PartialAmber
                            ObligationStatus.FULLY_SETTLED -> SettledGreen
                            ObligationStatus.OVERPAID -> OverpaidBlue
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp
                    )
                }
            }

            Text(
                text = "Recorded: ${DateTimeFormatter.formatRelativeTime(ob.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )

            if (!ob.notes.isNullOrBlank()) {
                Text(
                    text = "Note: ${ob.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else if (!ob.voiceTranscript.isNullOrBlank()) {
                Text(
                    text = "\"${ob.voiceTranscript}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }

            // Financial Breakdown Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Received: ${receivedMoney.formatRupees()}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SettledGreen
                )
                Text(
                    text = "Remaining: ${ob.remainingAmount.formatRupees()}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (ob.remainingAmount.isPositive) OpenRed else SettledGreen
                )
            }

            // Linked Settlements Detail (Audit Trail: Credit -> Payment -> Settlement)
            if (item.settlements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Linked Settlements (${item.settlements.size}):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                item.settlements.forEach { settlement ->
                    val appName = settlement.evidence?.paymentApp ?: "UPI"
                    val utr = settlement.evidence?.utrNumber
                    val utrText = if (!utr.isNullOrBlank()) " • UTR: $utr" else ""
                    val timeText = DateTimeFormatter.formatRelativeTime(settlement.reconciliation.reconciledAt)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SettledGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Settled ${settlement.reconciliation.settledAmount.formatRupees()} via $appName$utrText • $timeText",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerPaymentHistoryCard(item: LinkedSettlementUiModel) {
    val rec = item.reconciliation
    val ev = item.evidence
    val appName = ev?.paymentApp ?: "UPI"
    val utr = ev?.utrNumber

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_history_${rec.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceCardElevated
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtleDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceHigherDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = IQOOLime,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = rec.settledAmount.formatRupees(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = SettledGreen
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SurfaceHigherDark
                        ) {
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                    if (!utr.isNullOrBlank()) {
                        Text(
                            text = "UTR: $utr",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                    Text(
                        text = DateTimeFormatter.formatRelativeTime(rec.reconciledAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = SettledGreenContainer
            ) {
                Text(
                    text = "SETTLED",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SettledGreen,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun EmptySectionCard(
    title: String,
    message: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtleDark)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

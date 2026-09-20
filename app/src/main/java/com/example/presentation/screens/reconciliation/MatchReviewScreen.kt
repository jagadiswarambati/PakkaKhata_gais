package com.example.presentation.screens.reconciliation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.domain.model.Obligation
import com.example.domain.model.ObligationStatus
import com.example.domain.model.PaymentEvidence
import com.example.domain.model.SettlementOutcome
import com.example.domain.reconciliation.MatchCandidate
import com.example.domain.reconciliation.MatchDecision
import com.example.domain.usecase.ObligationWithCustomer
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchReviewScreen(
    evidenceId: Long,
    onNavigateToSettlementResult: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MatchReviewViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(evidenceId) {
        viewModel.load(evidenceId)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Match Review & Settle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Intelligent Reconciliation Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Ledger",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SurfaceElevatedDark
                )
            )
        },
        modifier = modifier.testTag("match_review_screen")
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
                        CircularProgressIndicator(color = IQOOLime)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Evaluating open customer obligations...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
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
                            tint = OpenRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = uiState.errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = OpenRed
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.load(evidenceId) },
                            colors = ButtonDefaults.buttonColors(containerColor = IQOOLime, contentColor = IQOOOnLime),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                else -> {
                    val evidence = uiState.evidence
                    if (evidence != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Duplicate warning banner if detected
                            if (uiState.isDuplicate) {
                                DuplicateWarningBanner(
                                    warningMessage = uiState.duplicateWarning
                                        ?: "This payment appears to have already been recorded."
                                )
                            }

                            // Settlement error banner if any
                            if (uiState.settlementError != null) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = OpenRedContainer
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, OpenRed.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = OpenRed
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = uiState.settlementError ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = OpenRed
                                        )
                                    }
                                }
                            }

                            // 1. Payment Received Card
                            PaymentReceivedCard(evidence = evidence)

                            // Visual Innovation Connector 1: Payment -> Match
                            InnovationConnector(
                                activeCandidate = uiState.activeCandidate,
                                decision = uiState.matchDecision
                            )

                            // 2. Original Credit Card
                            OriginalCreditCard(
                                candidate = uiState.activeCandidate,
                                onSelectObligation = { viewModel.openManualSelection() }
                            )

                            // 3. Match Explanation Card
                            MatchExplanationCard(
                                decision = uiState.matchDecision,
                                candidate = uiState.activeCandidate,
                                isRejected = uiState.isRejected
                            )

                            // 4. Action Buttons
                            ActionButtonsSection(
                                decision = uiState.matchDecision,
                                candidate = uiState.activeCandidate,
                                isDuplicate = uiState.isDuplicate,
                                isSettling = uiState.isSettling,
                                onConfirm = {
                                    viewModel.confirmSettlement(onSuccess = onNavigateToSettlementResult)
                                },
                                onChooseDifferent = { viewModel.openManualSelection() },
                                onReject = { viewModel.rejectMatch() },
                                onManualSelect = { viewModel.openManualSelection() }
                            )

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }

            // Manual Selection Bottom Sheet
            if (uiState.isManualSelectionOpen) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.closeManualSelection() },
                    sheetState = sheetState,
                    containerColor = SurfaceCardElevated,
                    modifier = Modifier.testTag("manual_obligation_list")
                ) {
                    ManualReassignmentSheet(
                        obligations = uiState.availableObligations,
                        searchQuery = uiState.searchQuery,
                        onSearchChange = { viewModel.updateSearchQuery(it) },
                        onSelect = { item -> viewModel.selectObligation(item) },
                        onClose = { viewModel.closeManualSelection() }
                    )
                }
            }
        }
    }
}

/**
 * Visual Connection between Payment Received and Original Credit.
 * Immediately communicates PakkaKhata's key innovation:
 * PAYMENT (₹300) ↓ PakkaKhata Matched This To ↓ OBLIGATION (₹500) ↓ RESULT (₹200)
 */
@Composable
private fun InnovationConnector(
    activeCandidate: MatchCandidate?,
    decision: MatchDecision
) {
    val confidencePct = ((activeCandidate?.overallScore ?: 0f) * 100).toInt().coerceIn(0, 100)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Vertical link line
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(10.dp)
                .background(IQOOLime.copy(alpha = 0.6f))
        )

        // Pill badge
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceHigherDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, IQOOLime.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = IQOOLime,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (activeCandidate != null) {
                        "PakkaKhata matched this to (${confidencePct}% match)"
                    } else {
                        "Manual obligation linking required"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (activeCandidate != null) IQOOLime else TextSecondary,
                    fontSize = 11.sp
                )
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = IQOOLime,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Bottom link line
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(10.dp)
                .background(IQOOLime.copy(alpha = 0.6f))
        )
    }
}

@Composable
fun DuplicateWarningBanner(
    warningMessage: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = OpenRedContainer
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, OpenRed),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("duplicate_warning_banner")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = OpenRed,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Duplicate Payment Warning",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OpenRed
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = warningMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun PaymentReceivedCard(
    evidence: PaymentEvidence,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceCardElevated
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMediumDark),
        modifier = modifier
            .fillMaxWidth()
            .testTag("payment_received_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = IQOOLimeContainer,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = IQOOLime,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PAYMENT RECEIVED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = IQOOLime,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceHigherDark
                ) {
                    Text(
                        text = evidence.paymentApp ?: "UPI Payment",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Evidence Thumbnail
                if (evidence.imagePath.isNotBlank()) {
                    val file = remember(evidence.imagePath) { File(evidence.imagePath) }
                    AsyncImage(
                        model = file,
                        contentDescription = "Payment Screenshot",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BorderMediumDark, RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = evidence.extractedAmount.formatRupees(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = IQOOLime
                    )
                    Text(
                        text = "Payer: ${evidence.extractedSenderName ?: "Unknown Sender"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "UTR / Reference",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = evidence.utrNumber ?: "Not Available",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Received At",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = evidence.timestamp.formatDateTime(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun OriginalCreditCard(
    candidate: MatchCandidate?,
    onSelectObligation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceCardElevated
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMediumDark),
        modifier = modifier
            .fillMaxWidth()
            .testTag("original_credit_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PartialAmberContainer,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = PartialAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ORIGINAL CREDIT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = PartialAmber,
                        letterSpacing = 1.sp
                    )
                }

                if (candidate != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (candidate.obligation.status) {
                            ObligationStatus.OPEN -> OpenRedContainer
                            ObligationStatus.PARTIALLY_SETTLED -> PartialAmberContainer
                            ObligationStatus.FULLY_SETTLED -> SettledGreenContainer
                            ObligationStatus.OVERPAID -> SurfaceHigherDark
                        }
                    ) {
                        Text(
                            text = candidate.obligation.status.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (candidate.obligation.status) {
                                ObligationStatus.OPEN -> OpenRed
                                ObligationStatus.PARTIALLY_SETTLED -> PartialAmber
                                ObligationStatus.FULLY_SETTLED -> SettledGreen
                                ObligationStatus.OVERPAID -> OverpaidBlue
                            },
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("status_badge"),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (candidate != null) {
                val obligation = candidate.obligation
                val customer = candidate.customer

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceHigherDark,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = IQOOLime
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Ledger Balance: ${customer.currentBalance.formatRupees()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Original Credit",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            fontSize = 10.sp
                        )
                        Text(
                            text = obligation.originalAmount.formatRupees(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Outstanding Due",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            fontSize = 10.sp
                        )
                        Text(
                            text = obligation.remainingAmount.formatRupees(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = OpenRed
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Recorded On",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            fontSize = 10.sp
                        )
                        Text(
                            text = obligation.createdAt.formatDateTime(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }

                if (!obligation.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Note: ${obligation.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            } else {
                // No candidate linked
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No open obligation currently linked.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onSelectObligation,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMediumDark)
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = IQOOLime)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select Open Obligation")
                    }
                }
            }
        }
    }
}

@Composable
fun MatchExplanationCard(
    decision: MatchDecision,
    candidate: MatchCandidate?,
    isRejected: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceCardElevated
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtleDark),
        modifier = modifier
            .fillMaxWidth()
            .testTag("match_explanation_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with match status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MATCH EXPLANATION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                val (badgeBg, badgeFg, label) = when {
                    isRejected -> Triple(SurfaceHigherDark, TextTertiary, "MATCH REJECTED")
                    decision == MatchDecision.AUTO_MATCH -> Triple(SettledGreenContainer, SettledGreen, "AUTO MATCH")
                    decision == MatchDecision.SUGGESTED_MATCH -> Triple(PartialAmberContainer, PartialAmber, "SUGGESTED MATCH")
                    else -> Triple(SurfaceHigherDark, TextTertiary, "NO MATCH FOUND")
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (candidate != null && !isRejected) {
                // Confidence gauge
                val confidencePct = (candidate.overallScore * 100).toInt().coerceIn(0, 100)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Confidence Level",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Text(
                        text = "$confidencePct%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (confidencePct >= 80) IQOOLime else PartialAmber
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Reasons bullet points
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    candidate.matchReasons.forEach { reason ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SettledGreen,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Proposed Settlement math breakdown
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtleDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "SETTLEMENT IMPACT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Settled Amount", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(
                                candidate.settlementPlan.settledAmount.formatRupees(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = SettledGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Remaining Due After Payment", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(
                                candidate.settlementPlan.newRemainingAmount.formatRupees(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (candidate.settlementPlan.newRemainingAmount.isPositive) OpenRed else SettledGreen
                            )
                        }
                        if (candidate.settlementPlan.outcome == SettlementOutcome.OVERPAID) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Overpaid / Excess Credit", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(
                                    candidate.settlementPlan.excessAmount.formatRupees(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = OverpaidBlue
                                )
                            }
                        }
                    }
                }
            } else {
                // No candidate or rejected
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRejected) {
                            "The suggested match was dismissed. You can select an open obligation below to link and settle this payment."
                        } else {
                            "No open debt matched the payer or amount with high confidence. You can manually assign this payment to an open customer obligation."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButtonsSection(
    decision: MatchDecision,
    candidate: MatchCandidate?,
    isDuplicate: Boolean,
    isSettling: Boolean,
    onConfirm: () -> Unit,
    onChooseDifferent: () -> Unit,
    onReject: () -> Unit,
    onManualSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (candidate != null) {
            // Confirm button
            Button(
                onClick = onConfirm,
                enabled = !isSettling && !isDuplicate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = IQOOLime,
                    contentColor = IQOOOnLime
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_settlement_button")
            ) {
                if (isSettling) {
                    CircularProgressIndicator(
                        color = IQOOOnLime,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (decision) {
                            MatchDecision.AUTO_MATCH -> "Confirm Settlement"
                            MatchDecision.SUGGESTED_MATCH -> "Confirm Match & Settle"
                            else -> "Apply Settlement"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Choose different obligation button
            OutlinedButton(
                onClick = onChooseDifferent,
                enabled = !isSettling,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderMediumDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("choose_different_obligation_button")
            ) {
                Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = IQOOLime)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Choose Different Obligation", fontWeight = FontWeight.SemiBold)
            }

            // Reject match button
            TextButton(
                onClick = onReject,
                enabled = !isSettling,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reject_match_button")
            ) {
                Text(
                    text = "Reject Match",
                    color = OpenRed,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // No candidate selected
            Button(
                onClick = onManualSelect,
                enabled = !isSettling && !isDuplicate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = IQOOLime,
                    contentColor = IQOOOnLime
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("choose_different_obligation_button")
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select Open Obligation to Settle",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ManualReassignmentSheet(
    obligations: List<ObligationWithCustomer>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelect: (ObligationWithCustomer) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filtered = remember(obligations, searchQuery) {
        if (searchQuery.isBlank()) obligations
        else {
            val q = searchQuery.trim().lowercase()
            obligations.filter {
                it.customer.name.lowercase().contains(q) ||
                        it.obligation.remainingAmount.rupeesWhole.toString().contains(q) ||
                        (it.obligation.notes?.lowercase()?.contains(q) == true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Select Open Obligation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Choose debt to allocate this payment to",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search customer name or amount...", color = TextTertiary) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = IQOOLime) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IQOOLime,
                unfocusedBorderColor = BorderMediumDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = IQOOLime
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "No open obligations found in ledger."
                    else "No obligations matching \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filtered, key = { it.obligation.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceDark
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtleDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item) }
                            .testTag("obligation_item_${item.obligation.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.customer.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Created: ${item.obligation.createdAt.formatDateTime()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary
                                )
                                if (!item.obligation.notes.isNullOrBlank()) {
                                    Text(
                                        text = item.obligation.notes,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = item.obligation.remainingAmount.formatRupees(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OpenRed
                                )
                                Text(
                                    text = "Original: ${item.obligation.originalAmount.formatRupees()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Long.formatDateTime(): String {
    return try {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(this))
    } catch (e: Exception) {
        ""
    }
}

package com.example.presentation.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.ObligationStatus
import com.example.domain.model.PaymentEvidence
import com.example.domain.theme.AppThemeMode
import com.example.domain.theme.ThemePreferences
import com.example.presentation.screens.settings.AppearanceSettingsSheet
import com.example.presentation.util.DateTimeFormatter
import com.example.ui.theme.PakkaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToPaymentCapture: () -> Unit = {},
    onNavigateToMatchReview: (Long) -> Unit = {},
    onNavigateToCustomerDetail: (Long) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = PakkaTheme.colors

    var showOfficeBridgeSheet by remember { mutableStateOf(false) }
    var showAppearanceSheet by remember { mutableStateOf(false) }

    val themePreferences = remember { ThemePreferences.getInstance(context) }
    val currentThemeMode by themePreferences.themeMode.collectAsState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceInput()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    val handleVoiceCreditClick = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.startVoiceInput()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val hasLedgerData = uiState.customerCount > 0 || uiState.openObligationsCount > 0 || uiState.recentLedgerItems.isNotEmpty()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        containerColor = colors.background,
        floatingActionButton = {
            if (hasLedgerData) {
                ExtendedFloatingActionButton(
                    onClick = handleVoiceCreditClick,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Credit Mic",
                            tint = colors.onLimePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = {
                        Text(
                            text = "Give Credit",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = colors.onLimePrimary,
                            letterSpacing = 0.2.sp
                        )
                    },
                    containerColor = colors.limePrimary,
                    contentColor = colors.onLimePrimary,
                    shape = RoundedCornerShape(18.dp),
                    elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp
                    ),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                        .testTag("add_credit_fab")
                )
            }
        }
    ) { innerPadding ->
        val bottomPadding = if (hasLedgerData) 88.dp else 24.dp
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Compact Native Header with Theme Toggle & On-Device AI Pill
            item {
                IQOONativeHeader(
                    currentThemeMode = currentThemeMode,
                    onOpenThemeSettings = { showAppearanceSheet = true },
                    onOpenOfficeBridge = { showOfficeBridgeSheet = true }
                )
            }

            // 2. Hero Financial Snapshot (Elevated Balance Card)
            item {
                IQOOHeroBalanceCard(uiState = uiState)
            }

            // 3. Compact Quick Stats Grid
            item {
                IQOOQuickStatsGrid(uiState = uiState)
            }

            // 4. Primary Quick Actions (Active Ledger only: Give Credit + Record Payment)
            if (hasLedgerData) {
                item {
                    IQOOPrimaryActionsSection(
                        pendingReviewCount = uiState.unreconciledEvidences.size,
                        onGiveCreditClick = handleVoiceCreditClick,
                        onRecordPaymentClick = onNavigateToPaymentCapture,
                        onReviewPaymentsClick = {
                            if (uiState.unreconciledEvidences.isNotEmpty()) {
                                onNavigateToMatchReview(uiState.unreconciledEvidences.first().id)
                            }
                        }
                    )
                }
            }

            // 5. Pending Payments Requiring Review Banner (if any)
            if (uiState.unreconciledEvidences.isNotEmpty()) {
                item {
                    IQOOPendingPaymentsSection(
                        pendingEvidences = uiState.unreconciledEvidences,
                        onReviewEvidence = onNavigateToMatchReview
                    )
                }
            }

            // 6. Customer Ledger Section Header & Filter Pills
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customer Ledger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            letterSpacing = 0.2.sp
                        )
                        if (hasLedgerData) {
                            Text(
                                text = "${uiState.recentLedgerItems.size} accounts",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary
                            )
                        }
                    }

                    if (hasLedgerData) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IQOORoundedFilterChip(
                                selected = uiState.activeFilter == LedgerFilter.ALL,
                                onClick = { viewModel.setLedgerFilter(LedgerFilter.ALL) },
                                label = "All (${uiState.customerCount})",
                                modifier = Modifier.testTag("filter_chip_all")
                            )
                            IQOORoundedFilterChip(
                                selected = uiState.activeFilter == LedgerFilter.ACTIVE_DUES,
                                onClick = { viewModel.setLedgerFilter(LedgerFilter.ACTIVE_DUES) },
                                label = "Active Dues (${uiState.customersWithOutstandingCount})",
                                highlightColor = if (uiState.customersWithOutstandingCount > 0) colors.openRed else null,
                                modifier = Modifier.testTag("filter_chip_active_dues")
                            )
                            IQOORoundedFilterChip(
                                selected = uiState.activeFilter == LedgerFilter.SETTLED,
                                onClick = { viewModel.setLedgerFilter(LedgerFilter.SETTLED) },
                                label = "Settled",
                                highlightColor = colors.settledGreen,
                                modifier = Modifier.testTag("filter_chip_settled")
                            )
                        }
                    }
                }
            }

            // 7. Ledger Obligations List or Empty State
            if (uiState.recentLedgerItems.isEmpty()) {
                item {
                    IQOOEmptyLedgerCard(
                        activeFilter = uiState.activeFilter,
                        showGiveFirstCredit = !hasLedgerData,
                        onGiveCreditClick = handleVoiceCreditClick
                    )
                }
            } else {
                items(uiState.recentLedgerItems, key = { it.obligation.id }) { item ->
                    IQOOLedgerObligationCard(
                        item = item,
                        onClick = { onNavigateToCustomerDetail(item.customerId) }
                    )
                }
            }

            // 8. Recently Settled Payments (Reconciliation Audit Trail)
            if (uiState.recentSettlements.isNotEmpty()) {
                item {
                    IQOORecentSettlementsSection(
                        recentSettlements = uiState.recentSettlements,
                        onCustomerClick = onNavigateToCustomerDetail
                    )
                }
            }

            // 9. Office Bridge & Innovation Showcase Card
            item {
                IQOOOfficeBridgeCard(
                    onClick = { showOfficeBridgeSheet = true }
                )
            }
        }
    }

    // Voice Credit Confirmation & Recording Bottom Sheet
    VoiceCreditBottomSheet(
        voiceState = uiState.voiceCreditState,
        onConfirm = viewModel::confirmCredit,
        onRetryVoice = viewModel::startVoiceInput,
        onDismiss = viewModel::dismissVoiceEntry,
        onStartManual = viewModel::startManualEntry,
        onToggleEdit = viewModel::toggleEdit,
        onNameChange = viewModel::updateCustomerName,
        onAmountChange = viewModel::updateAmount,
        onNoteChange = viewModel::updateNote
    )

    // Office Bridge & Rubric Showcase Sheet
    if (showOfficeBridgeSheet) {
        OfficeBridgeBottomSheet(
            uiState = uiState,
            onDismiss = { showOfficeBridgeSheet = false },
            onExportReport = { ctx -> viewModel.exportLedgerReport(ctx) },
            onLoadDemo = { viewModel.loadDemoScenario() },
            onResetLedger = { viewModel.resetLedgerForDemo() },
            onImportEvidenceImage = {
                showOfficeBridgeSheet = false
                onNavigateToPaymentCapture()
            }
        )
    }

    // Appearance Settings Bottom Sheet
    if (showAppearanceSheet) {
        AppearanceSettingsSheet(
            currentThemeMode = currentThemeMode,
            onThemeSelected = { newMode ->
                themePreferences.setThemeMode(newMode)
                showAppearanceSheet = false
            },
            onDismiss = { showAppearanceSheet = false }
        )
    }
}

/**
 * Compact system header inspired by iQOO/OriginOS system utilities.
 * Includes Theme Mode button and Offline AI Pill.
 */
@Composable
private fun IQOONativeHeader(
    currentThemeMode: AppThemeMode,
    onOpenThemeSettings: () -> Unit,
    onOpenOfficeBridge: () -> Unit
) {
    val colors = PakkaTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PakkaKhata",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.5).sp
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.limePrimary)
                )
            }
            Text(
                text = "The Ledger That Settles Itself",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                letterSpacing = 0.2.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Appearance & Theme Selector Icon Button
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenThemeSettings() }
                    .testTag("btn_appearance_settings")
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Theme Settings",
                        tint = colors.limePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // On-device AI & Security Pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenOfficeBridge() }
                    .testTag("btn_offline_ai_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = colors.limePrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Offline AI",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun IQOOOfficeBridgeCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PakkaTheme.colors

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCardElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("office_bridge_banner")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.limeContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Laptop,
                            contentDescription = null,
                            tint = if (colors.isDark) colors.limePrimary else colors.onLimeContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Office Bridge & Tools",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = colors.surface
                        ) {
                            Text(
                                text = "OFFLINE AI",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.limePrimary,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Shared clipboard, report export & 3-min demo",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Hero Financial Snapshot (Elevated Balance Card)
 * Visual centerpiece with structured hierarchy, depth, and positive states.
 */
@Composable
private fun IQOOHeroBalanceCard(uiState: HomeUiState) {
    val colors = PakkaTheme.colors
    val isDue = uiState.totalOutstanding.isPositive

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ledger_metrics_card"),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceCardElevated
        ),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    if (isDue) colors.borderAccent else colors.borderMedium,
                    colors.borderSubtle
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL OUTSTANDING",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    letterSpacing = 1.2.sp
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDue) colors.openRedContainer else colors.settledGreenContainer
                ) {
                    Text(
                        text = if (isDue) "${uiState.customersWithOutstandingCount} DUES PENDING" else "ALL CLEAR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDue) colors.openRedText else colors.settledGreenText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp
                    )
                }
            }

            // Big Amount Display
            Column {
                Text(
                    text = uiState.totalOutstanding.formatRupees(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDue) colors.textPrimary else colors.settledGreen,
                    letterSpacing = (-1).sp,
                    fontSize = 36.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isDue) {
                        "Across ${uiState.customersWithOutstandingCount} customers with active credit"
                    } else {
                        "All customer accounts settled • Ledger balanced"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            // Settlement progress indicator
            val totalCreditPaise = maxOf(
                1L,
                uiState.totalOutstanding.paise + uiState.totalSettledAmount.paise
            )
            val progressFraction = if (totalCreditPaise > 0) {
                (uiState.totalSettledAmount.paise.toFloat() / totalCreditPaise.toFloat()).coerceIn(0f, 1f)
            } else 1f

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = colors.settledGreen,
                    trackColor = colors.surface,
                    strokeCap = StrokeCap.Round
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Settled ${uiState.totalSettledAmount.formatRupees()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.settledGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${(progressFraction * 100).toInt()}% recovered",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textTertiary
                    )
                }
            }
        }
    }
}

/**
 * Compact 4-column quick stats system.
 */
@Composable
private fun IQOOQuickStatsGrid(uiState: HomeUiState) {
    val colors = PakkaTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IQOOStatTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.People,
            value = "${uiState.customerCount}",
            label = "Customers",
            tint = colors.textPrimary
        )
        IQOOStatTile(
            modifier = Modifier.weight(1f),
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            value = "${uiState.openObligationsCount}",
            label = "Active",
            tint = if (uiState.openObligationsCount > 0) colors.openRed else colors.textPrimary
        )
        IQOOStatTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.DoneAll,
            value = "${uiState.unreconciledEvidences.size}",
            label = "Review",
            tint = if (uiState.unreconciledEvidences.isNotEmpty()) colors.partialAmber else colors.textSecondary,
            highlight = uiState.unreconciledEvidences.isNotEmpty()
        )
        IQOOStatTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.CheckCircle,
            value = "${uiState.recentlySettledCount}",
            label = "Settled",
            tint = colors.settledGreen
        )
    }
}

@Composable
private fun IQOOStatTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    tint: Color,
    highlight: Boolean = false
) {
    val colors = PakkaTheme.colors

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (highlight) colors.partialAmber.copy(alpha = 0.5f) else colors.borderSubtle
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = tint,
                fontSize = 16.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Primary Actions (Dual Tactile Cards: Give Credit + Record Payment)
 */
@Composable
private fun IQOOPrimaryActionsSection(
    pendingReviewCount: Int,
    onGiveCreditClick: () -> Unit,
    onRecordPaymentClick: () -> Unit,
    onReviewPaymentsClick: () -> Unit
) {
    val colors = PakkaTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_actions_bar"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Give Credit Card (Primary Accent Treatment)
            Surface(
                onClick = onGiveCreditClick,
                shape = RoundedCornerShape(20.dp),
                color = colors.limeContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderAccent),
                modifier = Modifier
                    .weight(1f)
                    .height(84.dp)
                    .testTag("speak_credit_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.limePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = colors.onLimePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = "Give Credit",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Say who took credit",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Record Payment Card (Elevated Treatment)
            Surface(
                onClick = onRecordPaymentClick,
                shape = RoundedCornerShape(20.dp),
                color = colors.surfaceCardElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderMedium),
                modifier = Modifier
                    .weight(1f)
                    .height(84.dp)
                    .testTag("scan_evidence_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceHigher),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = colors.partialAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = "Record Pay",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Scan payment proof",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Quick shortcut to pending reviews if present
        if (pendingReviewCount > 0) {
            Surface(
                onClick = onReviewPaymentsClick,
                shape = RoundedCornerShape(14.dp),
                color = colors.partialAmberContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.partialAmber.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("review_payments_action_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.partialAmber)
                        )
                        Text(
                            text = "$pendingReviewCount payment(s) waiting for your confirmation",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.partialAmberText
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Review",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.partialAmberText
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = colors.partialAmberText,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Prominent card when payment evidence is waiting for review.
 */
@Composable
private fun IQOOPendingPaymentsSection(
    pendingEvidences: List<PaymentEvidence>,
    onReviewEvidence: (Long) -> Unit
) {
    val colors = PakkaTheme.colors

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCardElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.partialAmber.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("unreconciled_payments_banner")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.partialAmber)
                    )
                    Text(
                        text = "PAYMENT TO REVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.partialAmber,
                        letterSpacing = 1.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceHigher
                ) {
                    Text(
                        text = "${pendingEvidences.size} PENDING",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp
                    )
                }
            }

            pendingEvidences.take(3).forEach { evidence ->
                val appName = evidence.paymentApp ?: "UPI"
                val timeFormatted = DateTimeFormatter.formatRelativeTime(evidence.timestamp)

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = evidence.extractedAmount.formatRupees(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.limePrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = colors.surfaceHigher
                                ) {
                                    Text(
                                        text = appName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "From: ${evidence.extractedSenderName ?: "Customer"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            if (!evidence.utrNumber.isNullOrBlank()) {
                                Text(
                                    text = "UTR: ${evidence.utrNumber} • $timeFormatted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textTertiary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = { onReviewEvidence(evidence.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.limePrimary,
                                contentColor = colors.onLimePrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("reconcile_evidence_${evidence.id}")
                        ) {
                            Text("Review Match →", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Filter Chip styled as a refined pill.
 */
@Composable
private fun IQOORoundedFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    highlightColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val colors = PakkaTheme.colors

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = when {
            selected -> colors.surfaceHigher
            else -> colors.surfaceCard
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                selected && highlightColor != null -> highlightColor.copy(alpha = 0.6f)
                selected -> colors.borderAccent
                else -> colors.borderSubtle
            }
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(highlightColor ?: colors.limePrimary)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    selected && highlightColor != null -> highlightColor
                    selected -> colors.limePrimary
                    else -> colors.textSecondary
                }
            )
        }
    }
}

/**
 * Customer Ledger row item card.
 */
@Composable
private fun IQOOLedgerObligationCard(
    item: LedgerItemUiModel,
    onClick: () -> Unit
) {
    val colors = PakkaTheme.colors
    val receivedPaise = maxOf(0L, item.obligation.originalAmount.paise - item.obligation.remainingAmount.paise)
    val receivedMoney = com.example.domain.model.Money.fromPaise(receivedPaise)
    val formattedDate = DateTimeFormatter.formatRelativeTime(item.obligation.createdAt)

    val progressFraction = if (item.obligation.originalAmount.paise > 0) {
        (receivedPaise.toFloat() / item.obligation.originalAmount.paise.toFloat()).coerceIn(0f, 1f)
    } else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("obligation_item_${item.obligation.id}")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.limePrimary.copy(alpha = 0.2f)),
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCardElevated),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Customer Row Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Customer Avatar Squircle
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceHigher),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.customerName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.limePrimary
                        )
                    }
                    Column {
                        Text(
                            text = item.customerName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textTertiary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (item.obligation.status) {
                        ObligationStatus.OPEN -> colors.openRedContainer
                        ObligationStatus.PARTIALLY_SETTLED -> colors.partialAmberContainer
                        ObligationStatus.FULLY_SETTLED -> colors.settledGreenContainer
                        ObligationStatus.OVERPAID -> colors.surfaceHigher
                    }
                ) {
                    Text(
                        text = when (item.obligation.status) {
                            ObligationStatus.OPEN -> "OPEN"
                            ObligationStatus.PARTIALLY_SETTLED -> "PARTIAL"
                            ObligationStatus.FULLY_SETTLED -> "SETTLED"
                            ObligationStatus.OVERPAID -> "OVERPAID"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (item.obligation.status) {
                            ObligationStatus.OPEN -> colors.openRedText
                            ObligationStatus.PARTIALLY_SETTLED -> colors.partialAmberText
                            ObligationStatus.FULLY_SETTLED -> colors.settledGreenText
                            ObligationStatus.OVERPAID -> colors.overpaidBlueText
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp
                    )
                }
            }

            // Note or Transcript
            if (!item.obligation.notes.isNullOrBlank()) {
                Text(
                    text = item.obligation.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (!item.obligation.voiceTranscript.isNullOrBlank()) {
                Text(
                    text = "\"${item.obligation.voiceTranscript}\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Subtle Financial Progress Indicator
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (progressFraction >= 1f) colors.settledGreen else colors.limePrimary,
                trackColor = colors.surface,
                strokeCap = StrokeCap.Round
            )

            // Financial Values Breakdown Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Original Credit", style = MaterialTheme.typography.labelSmall, color = colors.textTertiary, fontSize = 10.sp)
                    Text(text = item.obligation.originalAmount.formatRupees(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Received", style = MaterialTheme.typography.labelSmall, color = colors.textTertiary, fontSize = 10.sp)
                    Text(text = receivedMoney.formatRupees(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.settledGreen)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Outstanding", style = MaterialTheme.typography.labelSmall, color = colors.textTertiary, fontSize = 10.sp)
                    Text(
                        text = item.obligation.remainingAmount.formatRupees(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (item.obligation.remainingAmount.isPositive) colors.openRed else colors.settledGreen
                    )
                }
            }

            // Subtle Tap Hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View History",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.limePrimary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colors.limePrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Refined, intentional empty state.
 */
@Composable
private fun IQOOEmptyLedgerCard(
    activeFilter: LedgerFilter,
    showGiveFirstCredit: Boolean = true,
    onGiveCreditClick: () -> Unit
) {
    val colors = PakkaTheme.colors

    val title = when (activeFilter) {
        LedgerFilter.ALL -> "Your ledger is ready"
        LedgerFilter.ACTIVE_DUES -> "All accounts settled"
        LedgerFilter.SETTLED -> "No settled accounts yet"
    }
    val description = when (activeFilter) {
        LedgerFilter.ALL -> "Record your first credit and PakkaKhata will keep track of the balance automatically."
        LedgerFilter.ACTIVE_DUES -> "All customer credits have been settled. No pending dues."
        LedgerFilter.SETTLED -> "Once payment evidence is matched with open credit, reconciled accounts appear here."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_ledger_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceHigher),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (activeFilter == LedgerFilter.ACTIVE_DUES) Icons.Default.DoneAll else Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (activeFilter == LedgerFilter.ACTIVE_DUES) colors.settledGreen else colors.limePrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (showGiveFirstCredit && activeFilter == LedgerFilter.ALL) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onGiveCreditClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.limePrimary,
                        contentColor = colors.onLimePrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    modifier = Modifier.testTag("give_first_credit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Give First Credit Mic",
                        modifier = Modifier.size(18.dp),
                        tint = colors.onLimePrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Give First Credit",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.onLimePrimary
                    )
                }
            }
        }
    }
}

/**
 * Audit trail showing recent auto-settlements.
 */
@Composable
private fun IQOORecentSettlementsSection(
    recentSettlements: List<RecentSettlementUiModel>,
    onCustomerClick: (Long) -> Unit
) {
    val colors = PakkaTheme.colors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_settlements_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recently Settled",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.settledGreenContainer
                ) {
                    Text(
                        text = "AUTO-RECONCILED",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.settledGreenText,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 9.sp
                    )
                }
            }

            recentSettlements.forEach { settlement ->
                val appText = settlement.paymentApp?.let { " via $it" } ?: ""
                val timeFormatted = DateTimeFormatter.formatRelativeTime(settlement.reconciledAt)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onCustomerClick(settlement.customerId) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colors.settledGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Settled ${settlement.settledAmount.formatRupees()} for ${settlement.customerName}$appText",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = timeFormatted,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

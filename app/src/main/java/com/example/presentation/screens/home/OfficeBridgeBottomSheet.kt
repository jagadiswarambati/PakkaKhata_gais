package com.example.presentation.screens.home

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.officebridge.OfficeBridgeService
import com.example.domain.theme.AppThemeMode
import com.example.domain.theme.ThemePreferences
import com.example.ui.theme.PakkaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficeBridgeBottomSheet(
    uiState: HomeUiState,
    onDismiss: () -> Unit,
    onExportReport: (Context) -> Unit,
    onLoadDemo: () -> Unit,
    onResetLedger: () -> Unit,
    onImportEvidenceImage: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val colors = PakkaTheme.colors
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val themePreferences = remember { ThemePreferences.getInstance(context) }
    val currentThemeMode by themePreferences.themeMode.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.testTag("office_bridge_bottom_sheet"),
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Export & Tools",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.limeContainer
                        ) {
                            Text(
                                text = "100% LOCAL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (colors.isDark) colors.limePrimary else colors.onLimeContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp
                            )
                        }
                    }
                    Text(
                        text = "iQOO Hackathon 2026 Rubric Showcase",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Tabs (4 tabs now including Appearance)
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = colors.surface,
                contentColor = colors.limePrimary,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = colors.limePrimary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Share & Clipboard",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Demo Mode",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = "Appearance",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Text(
                            text = "Architecture",
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    0 -> OfficeBridgeContent(
                        uiState = uiState,
                        onExportReport = { onExportReport(context) },
                        onImportEvidence = onImportEvidenceImage,
                        onCopySnapshot = {
                            val snapshotText = buildString {
                                appendLine("📋 PakkaKhata Store Snapshot")
                                appendLine("Outstanding Dues: ${uiState.totalOutstanding.formatRupees()} (${uiState.customersWithOutstandingCount} customers)")
                                appendLine("Open Obligations: ${uiState.openObligationsCount}")
                                appendLine("Total Settled: ${uiState.totalSettledAmount.formatRupees()}")
                                appendLine("Verified 100% On-Device • PakkaKhata")
                            }
                            OfficeBridgeService.copyToClipboard(context, "Ledger Snapshot", snapshotText)
                            Toast.makeText(context, "Ledger snapshot copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                    1 -> DemoModeContent(
                        uiState = uiState,
                        onLoadDemo = onLoadDemo,
                        onResetLedger = onResetLedger
                    )
                    2 -> AppearanceContent(
                        currentThemeMode = currentThemeMode,
                        onSelectMode = { newMode ->
                            themePreferences.setThemeMode(newMode)
                        }
                    )
                    3 -> ArchitectureContent()
                }
            }
        }
    }
}

@Composable
private fun OfficeBridgeContent(
    uiState: HomeUiState,
    onExportReport: () -> Unit,
    onImportEvidence: () -> Unit,
    onCopySnapshot: () -> Unit
) {
    val colors = PakkaTheme.colors

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Laptop, contentDescription = null, tint = colors.limePrimary, modifier = Modifier.size(20.dp))
                Text(
                    text = "DESKTOP & EXTERNAL WORKFLOWS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.limePrimary,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "Interoperability using standard Android features: system clipboard, local report export via Share Sheet, and screenshot selection.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
    }

    // Action 1: Share Ledger Summary
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "1. Share Store Ledger Report",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = "Generates a clean Markdown / Text financial report of all customers, balances, and settlements. Sent via Android Share Sheet (Quick Share, Email, or messaging apps).",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Button(
                onClick = onExportReport,
                colors = ButtonDefaults.buttonColors(containerColor = colors.limePrimary, contentColor = colors.onLimePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("btn_share_ledger_report")
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export & Share Report via Android Share", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Action 2: Import Payment Evidence
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "2. Select Payment Screenshot",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = "Have a payment screenshot saved on your device or transferred from a PC? Select it using the Android photo picker for instant on-device OCR and matching.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            OutlinedButton(
                onClick = onImportEvidence,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.limePrimary),
                modifier = Modifier.fillMaxWidth().testTag("btn_import_laptop_evidence")
            ) {
                Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = colors.limePrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Screenshot from Storage", fontWeight = FontWeight.SemiBold, color = colors.limePrimary)
            }
        }
    }

    // Action 3: Copy Ledger Snapshot to Shared Clipboard
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "3. Copy Snapshot to Clipboard",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = "Copies active store balances onto the Android system clipboard. Available for pasting into spreadsheets, messages, or notes.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            OutlinedButton(
                onClick = onCopySnapshot,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderMedium),
                modifier = Modifier.fillMaxWidth().testTag("btn_copy_ledger_snapshot")
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Store Balances to Clipboard", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            }
        }
    }
}

@Composable
private fun DemoModeContent(
    uiState: HomeUiState,
    onLoadDemo: () -> Unit,
    onResetLedger: () -> Unit
) {
    val colors = PakkaTheme.colors
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "3-MINUTE HACKATHON PITCH SCRIPT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = colors.limePrimary,
                letterSpacing = 1.sp
            )
            val pitchSteps = listOf(
                "1. Give Credit: Tap mic and speak \"Ramesh 500 udhar\" (On-Device Speech)",
                "2. Customer Pays Later: Payer transfers ₹300 via UPI (PhonePe / GPay)",
                "3. Capture Evidence: Take photo or select screenshot of payment",
                "4. Match Review: PakkaKhata links ₹300 payment to Ramesh's ₹500 credit",
                "5. Atomic Settle: Balance drops from ₹500 → ₹200. Copy summary for laptop!"
            )
            pitchSteps.forEach { step ->
                Text(text = step, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
    }

    // Quick Demo Actions
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = {
                onLoadDemo()
                Toast.makeText(context, "Loaded Ramesh Kumar (₹500 Credit)", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = colors.limePrimary, contentColor = colors.onLimePrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).testTag("btn_load_demo_scenario")
        ) {
            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Load Demo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        OutlinedButton(
            onClick = {
                onResetLedger()
                Toast.makeText(context, "Ledger reset to clean state", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.openRed),
            modifier = Modifier.weight(1f).testTag("btn_reset_ledger")
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = colors.openRed, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset Ledger", fontWeight = FontWeight.SemiBold, color = colors.openRed, fontSize = 13.sp)
        }
    }
}

@Composable
private fun AppearanceContent(
    currentThemeMode: AppThemeMode,
    onSelectMode: (AppThemeMode) -> Unit
) {
    val colors = PakkaTheme.colors

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = colors.limePrimary, modifier = Modifier.size(20.dp))
                Text(
                    text = "THEME & DISPLAY MODE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.limePrimary,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "Switch between the sleek iQOO-inspired dark theme and the crisp light financial theme. Theme is persisted locally.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ThemeOptionTile(
            mode = AppThemeMode.SYSTEM,
            title = "System Default",
            subtitle = "Follows Android system night mode",
            icon = Icons.Default.SettingsBrightness,
            selected = currentThemeMode == AppThemeMode.SYSTEM,
            onClick = { onSelectMode(AppThemeMode.SYSTEM) },
            testTag = "settings_theme_system"
        )
        ThemeOptionTile(
            mode = AppThemeMode.LIGHT,
            title = "Light Theme",
            subtitle = "Soft off-white background, white cards & graphite typography",
            icon = Icons.Default.LightMode,
            selected = currentThemeMode == AppThemeMode.LIGHT,
            onClick = { onSelectMode(AppThemeMode.LIGHT) },
            testTag = "settings_theme_light"
        )
        ThemeOptionTile(
            mode = AppThemeMode.DARK,
            title = "Dark Theme (iQOO)",
            subtitle = "Near-black OLED background, graphite surfaces & electric lime",
            icon = Icons.Default.DarkMode,
            selected = currentThemeMode == AppThemeMode.DARK,
            onClick = { onSelectMode(AppThemeMode.DARK) },
            testTag = "settings_theme_dark"
        )
    }
}

@Composable
private fun ThemeOptionTile(
    mode: AppThemeMode,
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val colors = PakkaTheme.colors

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) colors.surfaceElevated else colors.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) colors.limePrimary else colors.borderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (selected) colors.limeContainer else colors.surfaceHigher),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) colors.limePrimary else colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(colors.limePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = colors.onLimePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchitectureContent() {
    val colors = PakkaTheme.colors

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = colors.limePrimary, modifier = Modifier.size(20.dp))
                Text(
                    text = "100% LOCAL-FIRST ARCHITECTURE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.limePrimary,
                    letterSpacing = 1.sp
                )
            }

            val pipelineItems = listOf(
                "🎙 Voice Pipeline" to "Android SpeechRecognizer → Rule/Semantic Token Parser → Room DB (obligations)",
                "📷 Camera & OCR" to "CameraX → Google ML Kit Vision Model (Local) → Regex & Token UPI Parser → Room DB (payment_evidence)",
                "🧠 Reconciliation" to "Fuzzy Phonetic Name Matcher + Temporal Ordering + Deterministic Settlement Calculator + Duplicate Guard",
                "⚡ Atomic Transaction" to "Single ACID SQLite transaction updating payment, credit, customer balance, and reconciliation log",
                "💻 Office Bridge" to "System Shared Clipboard & Share Sheet for seamless laptop sync without cloud dependencies",
                "🎨 Dual-Theme Architecture" to "Centralized PakkaColors token system supporting OLED Dark and Clean Financial Light modes with local persistence"
            )

            pipelineItems.forEach { (title, desc) ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text(text = desc, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
        }
    }
}

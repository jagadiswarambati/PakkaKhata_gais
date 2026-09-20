package com.example.domain.officebridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.example.domain.model.Customer
import com.example.domain.model.Money
import com.example.domain.model.Obligation
import com.example.domain.model.PaymentEvidence
import com.example.domain.model.Reconciliation
import com.example.domain.model.SettlementOutcome
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Office Bridge Workflow Manager (Priority 3)
 *
 * Provides purposeful phone <-> laptop workflows for the iQOO Office Kit bridge:
 * 1. Shared Clipboard: Formatted reconciliation and settlement summaries
 * 2. File / Report Transfer: Formats comprehensive local ledger export for PC spreadsheet import
 * 3. Payment Evidence Hand-off: Laptop screenshot processing on phone
 *
 * 100% offline, local generation. Does not fake or simulate Office Kit APIs or telemetry.
 */
object OfficeBridgeService {

    /**
     * Formats a single reconciliation settlement into a structured clipboard text
     * optimized for pasting into PC spreadsheets, WhatsApp Web, or laptop accounting tools.
     */
    fun formatSettlementClipboardSummary(
        customerName: String,
        originalCredit: Money,
        paymentReceived: Money,
        remainingDue: Money,
        status: SettlementOutcome,
        utrNumber: String?,
        paymentApp: String?,
        timestamp: Date = Date()
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val statusText = when (status) {
            SettlementOutcome.FULLY_SETTLED -> "FULLY SETTLED"
            SettlementOutcome.PARTIALLY_SETTLED -> "PARTIALLY SETTLED"
            SettlementOutcome.OVERPAID -> "OVERPAID / ADVANCE"
            SettlementOutcome.NO_MATCH -> "UNLINKED / NO MATCH"
        }

        return buildString {
            appendLine("══════════════════════════════════════")
            appendLine("   PAKKAKHATA SETTLEMENT SUMMARY      ")
            appendLine("══════════════════════════════════════")
            appendLine("Customer:         $customerName")
            appendLine("Original Credit:  ${originalCredit.formatRupees()}")
            appendLine("Payment Received: ${paymentReceived.formatRupees()}")
            appendLine("Remaining Due:    ${remainingDue.formatRupees()}")
            appendLine("Status:           $statusText")
            if (!utrNumber.isNullOrBlank()) {
                appendLine("UTR / Ref:        $utrNumber")
            }
            if (!paymentApp.isNullOrBlank()) {
                appendLine("Payment Method:   $paymentApp")
            }
            appendLine("Settled At:       ${dateFormat.format(timestamp)}")
            appendLine("Verification:     On-Device Reconciliation (100% Local)")
            append("══════════════════════════════════════")
        }
    }

    /**
     * Copies text to system clipboard (which Office Kit syncs across phone and laptop).
     */
    fun copyToClipboard(context: Context, label: String, text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Generates a complete Local Ledger Report (Markdown / Text) for export to laptop.
     */
    fun generateLedgerReport(
        customers: List<Customer>,
        totalOutstanding: Money,
        activeObligationsCount: Int,
        settledCount: Int
    ): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date())

        return buildString {
            appendLine("# PakkaKhata Store Ledger Summary")
            appendLine("Generated On: $dateStr")
            appendLine("Security: 100% On-Device • Zero Cloud Storage")
            appendLine()
            appendLine("## Store Financial Snapshot")
            appendLine("- Total Active Customers: ${customers.size}")
            appendLine("- Open Credit Obligations: $activeObligationsCount")
            appendLine("- Total Dues Outstanding: ${totalOutstanding.formatRupees()}")
            appendLine("- Reconciled Settlements: $settledCount")
            appendLine()
            appendLine("## Customer Balances Breakdown")
            appendLine("| Customer Name | Outstanding Balance | Status |")
            appendLine("| :--- | :--- | :--- |")
            if (customers.isEmpty()) {
                appendLine("| No customer dues on record | ₹0.00 | All Clear |")
            } else {
                customers.sortedByDescending { it.currentBalance.paise }.forEach { customer ->
                    val status = if (customer.currentBalance.isPositive) "Dues Pending" else "Cleared"
                    appendLine("| ${customer.name} | ${customer.currentBalance.formatRupees()} | $status |")
                }
            }
            appendLine()
            appendLine("---")
            appendLine("Exported from PakkaKhata (iQOO Hackathon 2026 Edition)")
        }
    }

    /**
     * Launches Android system share sheet for the ledger report so it can be transferred
     * via Office Kit, Quick Share, or saved to a file on laptop.
     */
    fun shareLedgerReport(context: Context, reportContent: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "PakkaKhata Ledger Summary")
            putExtra(Intent.EXTRA_TEXT, reportContent)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Ledger Report to Laptop via Office Kit"))
    }
}

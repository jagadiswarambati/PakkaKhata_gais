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
 * System Clipboard & Share Service
 *
 * Provides phone-to-PC/external workflows using standard Android capabilities:
 * 1. System Clipboard: Formatted reconciliation and settlement summaries
 * 2. File / Report Transfer: Formats comprehensive local ledger export for PC spreadsheet import
 * 3. Payment Evidence Hand-off: Screenshot selection and OCR on phone
 *
 * 100% offline, local generation using standard Android ClipboardManager and Intent.ACTION_SEND.
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
     * Copies text to system clipboard (available for pasting or cross-device sync).
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
     * Generates a complete Local Ledger Report (Markdown / Text) for export.
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
     * Launches Android system share sheet for the ledger report so it can be shared
     * via Quick Share, email, messaging apps, or saved locally.
     */
    fun shareLedgerReport(context: Context, reportContent: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "PakkaKhata Ledger Summary")
            putExtra(Intent.EXTRA_TEXT, reportContent)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Ledger Report via Android Share"))
    }
}

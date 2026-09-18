package com.example.domain.perception

import com.example.domain.model.Money
import java.util.regex.Pattern

data class ParsedPaymentEvidence(
    val extractedAmount: Money?,
    val extractedSenderName: String?,
    val utrNumber: String?,
    val paymentApp: String?,
    val rawText: String
)

/**
 * Deterministic local parser that extracts structured payment details from raw OCR text.
 * Runs 100% offline without cloud APIs or network access.
 */
object PaymentEvidenceParser {

    // Common payment apps
    private val APP_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(?:Google\\s*Pay|GPay)\\b") to "Google Pay",
        Pattern.compile("(?i)\\bPhonePe\\b") to "PhonePe",
        Pattern.compile("(?i)\\bPaytm\\b") to "Paytm",
        Pattern.compile("(?i)\\bBHIM\\b") to "BHIM",
        Pattern.compile("(?i)\\bCRED\\b") to "CRED",
        Pattern.compile("(?i)\\bAmazon\\s*Pay\\b") to "Amazon Pay"
    )

    // Regex for explicit currency amounts: ₹300, ₹ 300, Rs 300, Rs. 1,200, INR 250, etc.
    private val CURRENCY_AMOUNT_REGEX = Pattern.compile(
        "(?i)(?:₹|rs\\.?|inr)\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    // Regex for standalone amounts following keywords like Received, Paid, Amount: e.g. "Received ₹500" or "Amount: 300.00"
    private val CONTEXTUAL_AMOUNT_REGEX = Pattern.compile(
        "(?i)(?:received|paid|amount|total)\\s*(?:of|is|:)?\\s*(?:₹|rs\\.?|inr)?\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    // Regex for standalone line amounts formatted like 300.00 or 1,200.00
    private val STANDALONE_AMOUNT_LINE_REGEX = Pattern.compile(
        "^\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{2})|[0-9]{1,6}\\.[0-9]{2})\\s*$"
    )

    // UTR / Reference number anchors
    private val UTR_REGEX = Pattern.compile(
        "(?i)(?:UPI\\s*Ref(?:erence)?(?:\\s*No)?|UTR|Transaction\\s*ID|Txn\\s*ID|Reference\\s*No|Ref\\s*No)[:\\s#]*([A-Za-z0-9]{6,25})",
        Pattern.CASE_INSENSITIVE
    )

    // Secondary standalone 12-digit UPI reference pattern if preceded or followed by reference context
    private val STANDALONE_12_DIGIT_REF = Pattern.compile(
        "(?i)(?:ref|rrn|txn|id|no)[:\\s]*([0-9]{12})\\b"
    )

    // Sender anchors: "Paid by", "Received from", "From", "Sent by"
    private val SENDER_LINE_REGEX = Pattern.compile(
        "(?i)^(?:paid\\s*by|received\\s*from|from|sent\\s*by)[:\\s]*(.+)$",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(rawText: String): ParsedPaymentEvidence {
        if (rawText.isBlank()) {
            return ParsedPaymentEvidence(
                extractedAmount = null,
                extractedSenderName = null,
                utrNumber = null,
                paymentApp = null,
                rawText = rawText
            )
        }

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        val amount = extractAmount(rawText, lines)
        val utr = extractUtr(rawText, lines)
        val sender = extractSender(lines, utr)
        val app = extractPaymentApp(rawText)

        return ParsedPaymentEvidence(
            extractedAmount = amount,
            extractedSenderName = sender,
            utrNumber = utr,
            paymentApp = app,
            rawText = rawText
        )
    }

    private fun extractAmount(rawText: String, lines: List<String>): Money? {
        // 1. Explicit currency symbol search (₹300, Rs 500, Rs. 1,200, INR 250)
        val currencyMatcher = CURRENCY_AMOUNT_REGEX.matcher(rawText)
        var firstValidAmount: Money? = null
        while (currencyMatcher.find()) {
            val amountStr = currencyMatcher.group(1)
            val parsed = parseAmountStringToMoney(amountStr)
            if (parsed != null && parsed.isPositive) {
                // If this amount isn't just an ID (e.g. not a 12 digit reference number)
                if (parsed.paise in 100..5000000000L) { // ₹1 to ₹50,00,000
                    firstValidAmount = parsed
                    break
                }
            }
        }
        if (firstValidAmount != null) return firstValidAmount

        // 2. Contextual amounts (Received 500, Paid 300)
        val contextualMatcher = CONTEXTUAL_AMOUNT_REGEX.matcher(rawText)
        while (contextualMatcher.find()) {
            val amountStr = contextualMatcher.group(1)
            val parsed = parseAmountStringToMoney(amountStr)
            if (parsed != null && parsed.isPositive && parsed.paise in 100..5000000000L) {
                return parsed
            }
        }

        // 3. Standalone lines formatted strictly as decimal currency (e.g. 300.00, 1,200.50)
        for (line in lines) {
            val standaloneMatcher = STANDALONE_AMOUNT_LINE_REGEX.matcher(line)
            if (standaloneMatcher.matches()) {
                val amountStr = standaloneMatcher.group(1)
                val parsed = parseAmountStringToMoney(amountStr)
                if (parsed != null && parsed.isPositive && parsed.paise in 100..5000000000L) {
                    return parsed
                }
            }
        }

        return null
    }

    private fun parseAmountStringToMoney(amountStr: String?): Money? {
        if (amountStr.isNullOrBlank()) return null
        return try {
            val clean = amountStr.replace(",", "").trim()
            val parts = clean.split(".")
            val rupees = parts[0].toLongOrNull() ?: return null
            val paise = if (parts.size > 1) {
                val fractional = parts[1].padEnd(2, '0').take(2)
                val fractionalPaise = fractional.toLongOrNull() ?: 0L
                (rupees * 100L) + fractionalPaise
            } else {
                rupees * 100L
            }
            if (paise <= 0L) null else Money.fromPaise(paise)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractUtr(rawText: String, lines: List<String>): String? {
        val utrMatcher = UTR_REGEX.matcher(rawText)
        if (utrMatcher.find()) {
            val match = utrMatcher.group(1)?.trim()
            if (!match.isNullOrBlank() && match.length >= 6) {
                return match
            }
        }

        // Check if label was on one line and value on the next line
        for (i in 0 until lines.size - 1) {
            val line = lines[i]
            if (line.matches(Regex("(?i)^(?:UPI\\s*Ref(?:erence)?(?:\\s*No)?|UTR|Transaction\\s*ID|Txn\\s*ID|Reference\\s*No|Ref\\s*No)[:\\s#]*$"))) {
                val nextLine = lines[i + 1].trim()
                val cleanNext = nextLine.replace(Regex("[^A-Za-z0-9]"), "")
                if (cleanNext.length >= 6) {
                    return cleanNext
                }
            }
        }

        // Secondary search for 12 digit standalone ref
        val standaloneMatcher = STANDALONE_12_DIGIT_REF.matcher(rawText)
        if (standaloneMatcher.find()) {
            val match = standaloneMatcher.group(1)?.trim()
            if (!match.isNullOrBlank()) {
                return match
            }
        }

        return null
    }

    private fun extractSender(lines: List<String>, utr: String?): String? {
        for (i in lines.indices) {
            val line = lines[i]

            // Check if line matches "Paid by [Name]" or "Received from [Name]" or "From [Name]"
            val matcher = SENDER_LINE_REGEX.matcher(line)
            if (matcher.find()) {
                val captured = matcher.group(1)?.trim()
                if (!captured.isNullOrBlank()) {
                    val clean = cleanSenderName(captured, utr)
                    if (clean != null) return clean
                }

                // If anchor was alone on this line (e.g. "Paid by:" or "From:"), check next line
                if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1].trim()
                    val clean = cleanSenderName(nextLine, utr)
                    if (clean != null) return clean
                }
            }
        }

        // Fallback: check lines between "Payment received" and the amount or app
        for (i in 0 until lines.size - 1) {
            val line = lines[i]
            if (line.equals("payment received", ignoreCase = true) || line.equals("received", ignoreCase = true)) {
                val candidate = lines[i + 1]
                val clean = cleanSenderName(candidate, utr)
                if (clean != null) return clean
            }
        }

        return null
    }

    private fun cleanSenderName(rawName: String, utr: String?): String? {
        var name = rawName.trim()

        // Strip trailing colon or dashes
        name = name.removePrefix(":").removePrefix("-").trim()

        // Remove trailing UPI ID in parenthesis or suffix: "Ramesh Kumar (ramesh@okaxis)" -> "Ramesh Kumar"
        name = name.replace(Regex("\\s*\\([^)]*@[^)]*\\)"), "")
        // Remove trailing UPI ID "ramesh@oksbi" if name is followed by UPI ID
        if (name.contains(" ")) {
            name = name.replace(Regex("\\s+[a-zA-Z0-9.\\-_]+@[a-zA-Z0-9]+"), "")
        }

        // If the name is identical to the UTR, reject
        if (utr != null && name.equals(utr, ignoreCase = true)) return null

        // If the name is just an amount or contains currency symbols, reject
        if (name.contains("₹") || name.startsWith("Rs", ignoreCase = true) || name.startsWith("INR", ignoreCase = true)) {
            return null
        }

        // Filter out known non-name strings
        val lower = name.lowercase()
        val blacklisted = listOf(
            "payment successful", "successful", "completed", "failed", "pending",
            "google pay", "gpay", "phonepe", "paytm", "bhim", "cred",
            "upi ref", "transaction id", "utr", "state bank", "hdfc", "icici", "axis",
            "banking name", "debited from", "credited to", "view details", "check balance"
        )
        if (blacklisted.any { lower == it || lower.startsWith("$it:") }) {
            return null
        }

        // If name contains only numbers or symbols, reject
        if (name.all { it.isDigit() || it.isWhitespace() || it == '-' || it == ':' }) {
            return null
        }

        // Must have at least 2 letters
        val letterCount = name.count { it.isLetter() }
        if (letterCount < 2) return null

        // Format to Title Case cleanly
        return name.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private fun extractPaymentApp(rawText: String): String? {
        for ((pattern, appName) in APP_PATTERNS) {
            if (pattern.matcher(rawText).find()) {
                return appName
            }
        }
        return null
    }
}

package com.example

import com.example.domain.model.Money
import com.example.domain.perception.PaymentEvidenceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentEvidenceParserTest {

    // ==========================================
    // AMOUNT EXTRACTION TESTS
    // ==========================================

    @Test
    fun testAmount_rupeeSymbol() {
        val result1 = PaymentEvidenceParser.parse("Payment Successful ₹300")
        assertEquals(Money.fromRupees(300L), result1.extractedAmount)

        val result2 = PaymentEvidenceParser.parse("Payment Successful ₹ 300")
        assertEquals(Money.fromRupees(300L), result2.extractedAmount)

        val result3 = PaymentEvidenceParser.parse("₹1,200 Paid to Merchant")
        assertEquals(Money.fromRupees(1200L), result3.extractedAmount)

        val result4 = PaymentEvidenceParser.parse("Total: ₹1,500.00")
        assertEquals(Money.fromRupees(1500L), result4.extractedAmount)

        // OCR misreads of rupee symbol e.g. ? or = or F
        val result5 = PaymentEvidenceParser.parse("Payment of ?500 successful")
        assertEquals(Money.fromRupees(500L), result5.extractedAmount)

        val result6 = PaymentEvidenceParser.parse("Paid =350 to grocery")
        assertEquals(Money.fromRupees(350L), result6.extractedAmount)
    }

    @Test
    fun testAmount_rsAndInr() {
        val result1 = PaymentEvidenceParser.parse("Received Rs 500 from customer")
        assertEquals(Money.fromRupees(500L), result1.extractedAmount)

        val result2 = PaymentEvidenceParser.parse("Paid Rs. 1,200 successfully")
        assertEquals(Money.fromRupees(1200L), result2.extractedAmount)

        val result3 = PaymentEvidenceParser.parse("Amount: INR 250")
        assertEquals(Money.fromRupees(250L), result3.extractedAmount)

        val result4 = PaymentEvidenceParser.parse("Rs. 300.50 credited")
        assertEquals(Money.fromPaise(30050L), result4.extractedAmount)

        val result5 = PaymentEvidenceParser.parse("Re. 1 paid to test account")
        assertEquals(Money.fromRupees(1L), result5.extractedAmount)
    }

    @Test
    fun testAmount_contextualKeywords() {
        // Debited, Credited, Transferred, Sent
        val res1 = PaymentEvidenceParser.parse("Debited 500.00 from bank account")
        assertEquals(Money.fromRupees(500L), res1.extractedAmount)

        val res2 = PaymentEvidenceParser.parse("Credited 1200 to account")
        assertEquals(Money.fromRupees(1200L), res2.extractedAmount)

        val res3 = PaymentEvidenceParser.parse("Sent 450 to Ramesh")
        assertEquals(Money.fromRupees(450L), res3.extractedAmount)

        val res4 = PaymentEvidenceParser.parse("Transferred ₹ 2,500 successfully")
        assertEquals(Money.fromRupees(2500L), res4.extractedAmount)
    }

    @Test
    fun testAmount_standaloneLines() {
        val text1 = """
            Payment Received
            300.00
            GPay
        """.trimIndent()
        val result1 = PaymentEvidenceParser.parse(text1)
        assertEquals(Money.fromRupees(300L), result1.extractedAmount)

        val text2 = """
            Payment Successful
            500
            PhonePe
            UPI Ref: 123456789012
        """.trimIndent()
        val result2 = PaymentEvidenceParser.parse(text2)
        assertEquals(Money.fromRupees(500L), result2.extractedAmount)

        val text3 = """
            Transferred
            ₹ 1,250
            Paid by Ramesh
        """.trimIndent()
        val result3 = PaymentEvidenceParser.parse(text3)
        assertEquals(Money.fromRupees(1250L), result3.extractedAmount)
    }

    // ==========================================
    // SENDER NAME EXTRACTION TESTS
    // ==========================================

    @Test
    fun testSender_paidBy() {
        val text = "Payment Successful\n₹300\nPaid by Ramesh Kumar\nUPI Ref: 123456"
        val result = PaymentEvidenceParser.parse(text)
        assertEquals("Ramesh Kumar", result.extractedSenderName)
    }

    @Test
    fun testSender_receivedFrom() {
        val text = "Received from Ramesh\nAmount ₹500"
        val result = PaymentEvidenceParser.parse(text)
        assertEquals("Ramesh", result.extractedSenderName)
    }

    @Test
    fun testSender_from() {
        val text = "₹250 received\nFrom Ramesh K\nTxn ID: 9876543210"
        val result = PaymentEvidenceParser.parse(text)
        assertEquals("Ramesh K", result.extractedSenderName)
    }

    @Test
    fun testSender_withTrailingUpiId() {
        val text = "Paid by Ramesh Kumar (ramesh@okaxis)\n₹500"
        val result = PaymentEvidenceParser.parse(text)
        assertEquals("Ramesh Kumar", result.extractedSenderName)
    }

    // ==========================================
    // UTR / REFERENCE EXTRACTION TESTS
    // ==========================================

    @Test
    fun testUtr_formats() {
        val result1 = PaymentEvidenceParser.parse("UPI Ref No: 4291823910")
        assertEquals("4291823910", result1.utrNumber)

        val result2 = PaymentEvidenceParser.parse("Transaction ID 123456789012")
        assertEquals("123456789012", result2.utrNumber)

        val result3 = PaymentEvidenceParser.parse("UTR: 9876543210")
        assertEquals("9876543210", result3.utrNumber)

        val result4 = PaymentEvidenceParser.parse("Ref No: 334455667788")
        assertEquals("334455667788", result4.utrNumber)
    }

    // ==========================================
    // PAYMENT APPLICATION DETECTION TESTS
    // ==========================================

    @Test
    fun testPaymentApp_detection() {
        assertEquals("Google Pay", PaymentEvidenceParser.parse("Paid via Google Pay ₹500").paymentApp)
        assertEquals("Google Pay", PaymentEvidenceParser.parse("GPay transaction ₹500").paymentApp)
        assertEquals("PhonePe", PaymentEvidenceParser.parse("PhonePe - Payment Successful ₹300").paymentApp)
        assertEquals("Paytm", PaymentEvidenceParser.parse("Paytm payment received ₹400").paymentApp)
        assertEquals("BHIM", PaymentEvidenceParser.parse("BHIM UPI ₹200").paymentApp)
    }

    // ==========================================
    // REALISTIC COMBINED SAMPLES
    // ==========================================

    @Test
    fun testRealistic_phonePeSample() {
        val ocr = """
            Payment Successful
            ₹300
            Paid by Ramesh Kumar
            UPI Ref No: 4291823910
            PhonePe
        """.trimIndent()

        val parsed = PaymentEvidenceParser.parse(ocr)
        assertEquals(Money.fromRupees(300L), parsed.extractedAmount)
        assertEquals("Ramesh Kumar", parsed.extractedSenderName)
        assertEquals("4291823910", parsed.utrNumber)
        assertEquals("PhonePe", parsed.paymentApp)
    }

    @Test
    fun testRealistic_googlePaySample() {
        val ocr = """
            Received ₹500
            From Ramesh
            Transaction ID 123456789012
            Google Pay
        """.trimIndent()

        val parsed = PaymentEvidenceParser.parse(ocr)
        assertEquals(Money.fromRupees(500L), parsed.extractedAmount)
        assertEquals("Ramesh", parsed.extractedSenderName)
        assertEquals("123456789012", parsed.utrNumber)
        assertEquals("Google Pay", parsed.paymentApp)
    }

    @Test
    fun testRealistic_paytmSample() {
        val ocr = """
            Payment received
            Ramesh Kumar
            ₹300
            Paytm
            Ref No: 9988776655
        """.trimIndent()

        val parsed = PaymentEvidenceParser.parse(ocr)
        assertEquals(Money.fromRupees(300L), parsed.extractedAmount)
        assertEquals("Ramesh Kumar", parsed.extractedSenderName)
        assertEquals("9988776655", parsed.utrNumber)
        assertEquals("Paytm", parsed.paymentApp)
    }

    // ==========================================
    // MISSING & INVALID FIELDS TESTS
    // ==========================================

    @Test
    fun testMissing_noSender() {
        val ocr = """
            Payment Successful
            ₹500
            UPI Ref: 44556677
            PhonePe
        """.trimIndent()
        val parsed = PaymentEvidenceParser.parse(ocr)
        assertEquals(Money.fromRupees(500L), parsed.extractedAmount)
        assertNull(parsed.extractedSenderName)
        assertEquals("44556677", parsed.utrNumber)
    }

    @Test
    fun testMissing_noUtr() {
        val ocr = """
            Paid by Ramesh
            ₹250
        """.trimIndent()
        val parsed = PaymentEvidenceParser.parse(ocr)
        assertEquals(Money.fromRupees(250L), parsed.extractedAmount)
        assertEquals("Ramesh", parsed.extractedSenderName)
        assertNull(parsed.utrNumber)
    }

    @Test
    fun testMissing_noApp() {
        val ocr = """
            Payment Successful
            ₹700
            Paid by Anita
        """.trimIndent()
        val parsed = PaymentEvidenceParser.parse(ocr)
        assertEquals(Money.fromRupees(700L), parsed.extractedAmount)
        assertEquals("Anita", parsed.extractedSenderName)
        assertNull(parsed.paymentApp)
    }

    @Test
    fun testMissing_noAmount() {
        val ocr = """
            Payment Successful
            Paid by Ramesh
            UPI Ref: 123456
        """.trimIndent()
        val parsed = PaymentEvidenceParser.parse(ocr)
        assertNull(parsed.extractedAmount)
        assertEquals("Ramesh", parsed.extractedSenderName)
    }

    @Test
    fun testUnrelatedOcrText() {
        val ocr = "Good morning grocery store today's special offer on apples"
        val parsed = PaymentEvidenceParser.parse(ocr)
        assertNull(parsed.extractedAmount)
        assertNull(parsed.extractedSenderName)
        assertNull(parsed.utrNumber)
        assertNull(parsed.paymentApp)
    }

    @Test
    fun testBlankText() {
        val parsed = PaymentEvidenceParser.parse("   \n\t  ")
        assertNull(parsed.extractedAmount)
        assertNull(parsed.extractedSenderName)
        assertNull(parsed.utrNumber)
        assertNull(parsed.paymentApp)
    }
}

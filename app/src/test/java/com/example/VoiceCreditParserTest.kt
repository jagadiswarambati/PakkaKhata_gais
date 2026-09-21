package com.example

import com.example.domain.model.Money
import com.example.domain.perception.VoiceCreditParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCreditParserTest {

    // ==========================================
    // ENGLISH TESTS
    // ==========================================

    @Test
    fun testEnglish_nameAndAmount() {
        val result = VoiceCreditParser.parse("Ramesh 500")
        assertTrue(result.isSuccess)
        val entry = result.getOrThrow()
        assertEquals("Ramesh", entry.customerName)
        assertEquals(Money.fromRupees(500L), entry.amount)
        assertNull(entry.optionalNote)
    }

    @Test
    fun testEnglish_nameAmountAndCreditKeyword() {
        val result = VoiceCreditParser.parse("Ramesh 500 credit")
        assertTrue(result.isSuccess)
        val entry = result.getOrThrow()
        assertEquals("Ramesh", entry.customerName)
        assertEquals(Money.fromRupees(500L), entry.amount)
        assertNull(entry.optionalNote)
    }

    @Test
    fun testEnglish_jagadiswar500Credit() {
        val result = VoiceCreditParser.parse("Jagadiswar 500 credit")
        assertTrue(result.isSuccess)
        val entry = result.getOrThrow()
        assertEquals("Jagadiswar", entry.customerName)
        assertEquals(Money.fromRupees(500L), entry.amount)
        assertNull(entry.optionalNote)
    }

    @Test
    fun testEnglish_nameAmountAndItemNote() {
        val result = VoiceCreditParser.parse("Ramesh 250 doodh")
        assertTrue(result.isSuccess)
        val entry = result.getOrThrow()
        assertEquals("Ramesh", entry.customerName)
        assertEquals(Money.fromRupees(250L), entry.amount)
        assertEquals("doodh", entry.optionalNote)
    }

    @Test
    fun testEnglish_spokenNumberWords() {
        val result1 = VoiceCreditParser.parse("Suresh one thousand")
        assertTrue(result1.isSuccess)
        assertEquals(Money.fromRupees(1000L), result1.getOrThrow().amount)

        val result2 = VoiceCreditParser.parse("Anita two hundred fifty")
        assertTrue(result2.isSuccess)
        assertEquals(Money.fromRupees(250L), result2.getOrThrow().amount)

        val result3 = VoiceCreditParser.parse("Mahesh five thousand")
        assertTrue(result3.isSuccess)
        assertEquals(Money.fromRupees(5000L), result3.getOrThrow().amount)
    }

    // ==========================================
    // HINDI / HINGLISH TESTS
    // ==========================================

    @Test
    fun testHindi_rameshKoPaanchSauRupayeUdharDiya() {
        val result = VoiceCreditParser.parse("Ramesh ko paanch sau rupaye udhar diya")
        assertTrue(result.isSuccess)
        val entry = result.getOrThrow()
        assertEquals("Ramesh", entry.customerName)
        assertEquals(Money.fromRupees(500L), entry.amount)
        assertNull(entry.optionalNote)
    }

    @Test
    fun testHindi_ramesh500Udhar() {
        val result = VoiceCreditParser.parse("Ramesh 500 udhar")
        assertTrue(result.isSuccess)
        val entry = result.getOrThrow()
        assertEquals("Ramesh", entry.customerName)
        assertEquals(Money.fromRupees(500L), entry.amount)
        assertNull(entry.optionalNote)
    }

    @Test
    fun testHindi_sureshKoEkHazaarRupayeUdharDiya() {
        val result = VoiceCreditParser.parse("Suresh ko ek hazaar rupaye udhar diya")
        assertTrue(result.isSuccess)
        val entry = result.getOrThrow()
        assertEquals("Suresh", entry.customerName)
        assertEquals(Money.fromRupees(1000L), entry.amount)
        assertNull(entry.optionalNote)
    }

    @Test
    fun testHindi_compoundSpokenNumbers() {
        val result1 = VoiceCreditParser.parse("Vikram dhai sau doodh")
        assertTrue(result1.isSuccess)
        assertEquals("Vikram", result1.getOrThrow().customerName)
        assertEquals(Money.fromRupees(250L), result1.getOrThrow().amount)
        assertEquals("doodh", result1.getOrThrow().optionalNote)

        val result2 = VoiceCreditParser.parse("Kiran dedh sau")
        assertTrue(result2.isSuccess)
        assertEquals("Kiran", result2.getOrThrow().customerName)
        assertEquals(Money.fromRupees(150L), result2.getOrThrow().amount)

        val result3 = VoiceCreditParser.parse("Rohit dhai hazaar udhar")
        assertTrue(result3.isSuccess)
        assertEquals("Rohit", result3.getOrThrow().customerName)
        assertEquals(Money.fromRupees(2500L), result3.getOrThrow().amount)
    }

    // ==========================================
    // NORMALIZATION TESTS
    // ==========================================

    @Test
    fun testNormalization_allCaseVariations() {
        val rUpper = VoiceCreditParser.parse("RAMESH 500")
        val rTitle = VoiceCreditParser.parse("Ramesh 500")
        val rLower = VoiceCreditParser.parse("ramesh 500")

        assertTrue(rUpper.isSuccess)
        assertTrue(rTitle.isSuccess)
        assertTrue(rLower.isSuccess)

        assertEquals("ramesh", rUpper.getOrThrow().normalizedCustomerName)
        assertEquals("ramesh", rTitle.getOrThrow().normalizedCustomerName)
        assertEquals("ramesh", rLower.getOrThrow().normalizedCustomerName)
    }

    @Test
    fun testNormalization_nameWithSurname() {
        val result = VoiceCreditParser.parse("Ramesh Kumar 500 credit")
        assertTrue(result.isSuccess)
        val entry = result.getOrThrow()
        assertEquals("Ramesh Kumar", entry.customerName)
        assertEquals("ramesh kumar", entry.normalizedCustomerName)
        assertEquals(Money.fromRupees(500L), entry.amount)
    }

    // ==========================================
    // INVALID INPUT TESTS
    // ==========================================

    @Test
    fun testInvalid_missingName() {
        val result = VoiceCreditParser.parse("500 credit")
        assertTrue("Missing name must fail", result.isFailure)
    }

    @Test
    fun testInvalid_missingAmount() {
        val result = VoiceCreditParser.parse("Ramesh credit")
        assertTrue("Missing amount must fail", result.isFailure)
    }

    @Test
    fun testInvalid_zeroAmount() {
        val result = VoiceCreditParser.parse("Ramesh 0 credit")
        assertTrue("Zero amount must fail", result.isFailure)
    }

    @Test
    fun testInvalid_unrelatedSpeech() {
        val result = VoiceCreditParser.parse("Good morning how is the weather today")
        assertTrue("Unrelated speech must fail", result.isFailure)
    }

    @Test
    fun testInvalid_blankInput() {
        val result = VoiceCreditParser.parse("   ")
        assertTrue("Blank input must fail", result.isFailure)
    }
}

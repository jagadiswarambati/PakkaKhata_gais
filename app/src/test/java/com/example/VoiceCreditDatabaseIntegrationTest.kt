package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PakkaKhataDatabase
import com.example.data.repository.LedgerRepositoryImpl
import com.example.domain.model.Money
import com.example.domain.model.ObligationStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VoiceCreditDatabaseIntegrationTest {

    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var database: PakkaKhataDatabase
    private lateinit var repository: LedgerRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        dbFile = context.getDatabasePath("test_pakka_khata.db")
        if (dbFile.exists()) dbFile.delete()

        database = Room.databaseBuilder(context, PakkaKhataDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .build()
        repository = LedgerRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        database.close()
        if (dbFile.exists()) dbFile.delete()
    }

    @Test
    fun testVoiceCredit_createsNewCustomerAndObligation() = runBlocking {
        val transcript = "Ramesh ko paanch sau rupaye udhar diya"
        val amount = Money.fromRupees(500L)

        val result = repository.recordCreditObligation(
            customerName = "Ramesh",
            amount = amount,
            voiceTranscript = transcript,
            notes = null
        )

        assertTrue(result.isSuccess)
        val obligation = result.getOrThrow()

        // 1. Obligation fields
        assertEquals(amount, obligation.originalAmount)
        assertEquals(amount, obligation.remainingAmount)
        assertEquals(ObligationStatus.OPEN, obligation.status)
        assertEquals(transcript, obligation.voiceTranscript)

        // 2. Customer created & balance initialized
        val customer = repository.customerRepo.getCustomerByIdDirect(obligation.customerId)
        assertNotNull(customer)
        assertEquals("Ramesh", customer!!.name)
        assertEquals("ramesh", customer.normalizedName)
        assertEquals(amount, customer.currentBalance)
    }

    @Test
    fun testVoiceCredit_jagadiswar500CreditFlow() = runBlocking {
        val transcript = "Jagadiswar 500 credit"
        val parsed = com.example.domain.perception.VoiceCreditParser.parse(transcript)
        assertTrue(parsed.isSuccess)
        val entry = parsed.getOrThrow()

        val result = repository.recordCreditObligation(
            customerName = entry.customerName,
            amount = entry.amount,
            voiceTranscript = transcript,
            notes = entry.optionalNote
        )

        assertTrue(result.isSuccess)
        val obligation = result.getOrThrow()

        assertEquals(Money.fromRupees(500L), obligation.originalAmount)
        assertEquals(Money.fromRupees(500L), obligation.remainingAmount)
        assertEquals(ObligationStatus.OPEN, obligation.status)
        assertEquals("Jagadiswar 500 credit", obligation.voiceTranscript)

        val customer = repository.customerRepo.getCustomerByIdDirect(obligation.customerId)
        assertNotNull(customer)
        assertEquals("Jagadiswar", customer!!.name)
        assertEquals(Money.fromRupees(500L), customer.currentBalance)
    }

    @Test
    fun testVoiceCredit_reusesExistingCustomerCaseInsensitive() = runBlocking {
        // First credit: "Ramesh" ₹500
        val res1 = repository.recordCreditObligation(
            customerName = "Ramesh",
            amount = Money.fromRupees(500L),
            voiceTranscript = "Ramesh 500",
            notes = null
        )
        assertTrue(res1.isSuccess)
        val ob1 = res1.getOrThrow()

        // Second credit: "ramesh" ₹300 (lowercase)
        val res2 = repository.recordCreditObligation(
            customerName = "ramesh",
            amount = Money.fromRupees(300L),
            voiceTranscript = "ramesh 300 doodh",
            notes = "doodh"
        )
        assertTrue(res2.isSuccess)
        val ob2 = res2.getOrThrow()

        // Verify same customer ID was reused
        assertEquals("Same customer ID must be reused across case variations", ob1.customerId, ob2.customerId)

        // Customer count must still be exactly 1
        val customers = repository.customerRepo.getAllCustomers().first()
        assertEquals(1, customers.size)

        // Customer balance must be ₹500 + ₹300 = ₹800
        val updatedCustomer = repository.customerRepo.getCustomerByIdDirect(ob1.customerId)
        assertNotNull(updatedCustomer)
        assertEquals(Money.fromRupees(800L), updatedCustomer!!.currentBalance)

        // Both obligations are distinct OPEN obligations
        val openObs = repository.obligationRepo.getOpenObligationsDirect()
        assertEquals(2, openObs.size)
        assertEquals("doodh", ob2.notes)
    }

    @Test
    fun testVoiceCredit_persistsAcrossDatabaseRestart() = runBlocking {
        // Record credit obligation
        val recordResult = repository.recordCreditObligation(
            customerName = "Anita",
            amount = Money.fromRupees(1200L),
            voiceTranscript = "Anita 1200 udhar",
            notes = "udhar"
        )
        assertTrue(recordResult.isSuccess)
        val obId = recordResult.getOrThrow().id

        // Close database simulating app exit/process death
        database.close()

        // Reopen database from same file
        val reopenedDatabase = Room.databaseBuilder(
            context,
            PakkaKhataDatabase::class.java,
            dbFile.absolutePath
        ).allowMainThreadQueries().build()
        val reopenedRepo = LedgerRepositoryImpl(reopenedDatabase)

        try {
            val fetchedObligation = reopenedRepo.obligationRepo.getObligationByIdDirect(obId)
            assertNotNull("Obligation must survive database restart", fetchedObligation)
            assertEquals(Money.fromRupees(1200L), fetchedObligation!!.originalAmount)
            assertEquals(Money.fromRupees(1200L), fetchedObligation.remainingAmount)
            assertEquals(ObligationStatus.OPEN, fetchedObligation.status)
            assertEquals("Anita 1200 udhar", fetchedObligation.voiceTranscript)

            val customer = reopenedRepo.customerRepo.getCustomerByIdDirect(fetchedObligation.customerId)
            assertNotNull(customer)
            assertEquals("Anita", customer!!.name)
            assertEquals(Money.fromRupees(1200L), customer.currentBalance)
        } finally {
            reopenedDatabase.close()
        }
    }
}

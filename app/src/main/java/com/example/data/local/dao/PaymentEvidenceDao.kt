package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.PaymentEvidenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentEvidenceDao {

    @Query("SELECT * FROM payment_evidences ORDER BY timestamp DESC")
    fun getAllEvidence(): Flow<List<PaymentEvidenceEntity>>

    @Query("SELECT * FROM payment_evidences WHERE id = :id LIMIT 1")
    fun getEvidenceById(id: Long): Flow<PaymentEvidenceEntity?>

    @Query("SELECT * FROM payment_evidences WHERE id = :id LIMIT 1")
    suspend fun getEvidenceByIdDirect(id: Long): PaymentEvidenceEntity?

    @Query("SELECT * FROM payment_evidences WHERE utrNumber = :utrNumber LIMIT 1")
    suspend fun getEvidenceByUtr(utrNumber: String): PaymentEvidenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidence(evidence: PaymentEvidenceEntity): Long

    @Update
    suspend fun updateEvidence(evidence: PaymentEvidenceEntity)

    @Delete
    suspend fun deleteEvidence(evidence: PaymentEvidenceEntity)

    @Query("SELECT COUNT(*) FROM payment_evidences")
    fun getEvidenceCount(): Flow<Int>
}

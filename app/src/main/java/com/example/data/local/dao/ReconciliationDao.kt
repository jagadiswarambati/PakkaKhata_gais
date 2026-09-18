package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.ReconciliationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReconciliationDao {

    @Query("SELECT * FROM reconciliations ORDER BY reconciledAt DESC")
    fun getAllReconciliations(): Flow<List<ReconciliationEntity>>

    @Query("SELECT * FROM reconciliations WHERE obligationId = :obligationId ORDER BY reconciledAt DESC")
    fun getReconciliationsByObligation(obligationId: Long): Flow<List<ReconciliationEntity>>

    @Query("SELECT * FROM reconciliations WHERE evidenceId = :evidenceId ORDER BY reconciledAt DESC")
    fun getReconciliationsByEvidence(evidenceId: Long): Flow<List<ReconciliationEntity>>

    @Query("SELECT * FROM reconciliations WHERE id = :id LIMIT 1")
    fun getReconciliationById(id: Long): Flow<ReconciliationEntity?>

    @Query("SELECT * FROM reconciliations WHERE id = :id LIMIT 1")
    suspend fun getReconciliationByIdDirect(id: Long): ReconciliationEntity?

    @Query("SELECT * FROM reconciliations WHERE evidenceId = :evidenceId ORDER BY reconciledAt DESC")
    suspend fun getReconciliationsByEvidenceDirect(evidenceId: Long): List<ReconciliationEntity>

    @Query("SELECT * FROM reconciliations ORDER BY reconciledAt DESC")
    suspend fun getAllReconciliationsDirect(): List<ReconciliationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReconciliation(reconciliation: ReconciliationEntity): Long

    @Update
    suspend fun updateReconciliation(reconciliation: ReconciliationEntity)

    @Delete
    suspend fun deleteReconciliation(reconciliation: ReconciliationEntity)

    @Query("SELECT COUNT(*) FROM reconciliations")
    fun getReconciliationCount(): Flow<Int>

    @Query("SELECT SUM(settledAmountPaise) FROM reconciliations")
    fun getTotalSettledAmountPaise(): Flow<Long?>
}

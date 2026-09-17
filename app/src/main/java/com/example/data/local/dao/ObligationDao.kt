package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.ObligationEntity
import com.example.domain.model.ObligationStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ObligationDao {

    @Query("SELECT * FROM obligations ORDER BY createdAt DESC")
    fun getAllObligations(): Flow<List<ObligationEntity>>

    @Query("SELECT * FROM obligations WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getObligationsByCustomer(customerId: Long): Flow<List<ObligationEntity>>

    @Query("SELECT * FROM obligations WHERE customerId = :customerId ORDER BY createdAt DESC")
    suspend fun getObligationsByCustomerDirect(customerId: Long): List<ObligationEntity>

    @Query("SELECT * FROM obligations WHERE status IN ('OPEN', 'PARTIALLY_SETTLED') ORDER BY createdAt ASC")
    fun getOpenObligations(): Flow<List<ObligationEntity>>

    @Query("SELECT * FROM obligations WHERE status IN ('OPEN', 'PARTIALLY_SETTLED') ORDER BY createdAt ASC")
    suspend fun getOpenObligationsDirect(): List<ObligationEntity>

    @Query("SELECT * FROM obligations WHERE id = :id LIMIT 1")
    fun getObligationById(id: Long): Flow<ObligationEntity?>

    @Query("SELECT * FROM obligations WHERE id = :id LIMIT 1")
    suspend fun getObligationByIdDirect(id: Long): ObligationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObligation(obligation: ObligationEntity): Long

    @Update
    suspend fun updateObligation(obligation: ObligationEntity)

    @Query("UPDATE obligations SET remainingAmountPaise = :remainingAmountPaise, status = :status WHERE id = :obligationId")
    suspend fun updateObligationSettlement(obligationId: Long, remainingAmountPaise: Long, status: ObligationStatus)

    @Delete
    suspend fun deleteObligation(obligation: ObligationEntity)

    @Query("SELECT SUM(remainingAmountPaise) FROM obligations WHERE status IN ('OPEN', 'PARTIALLY_SETTLED')")
    fun getTotalOutstandingPaise(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM obligations WHERE status IN ('OPEN', 'PARTIALLY_SETTLED')")
    fun getOpenObligationCount(): Flow<Int>
}

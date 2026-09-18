package com.example.data.repository

import com.example.data.local.dao.ReconciliationDao
import com.example.data.local.entities.ReconciliationEntity
import com.example.domain.model.Money
import com.example.domain.model.Reconciliation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ReconciliationRepository {
    fun getAllReconciliations(): Flow<List<Reconciliation>>
    fun getReconciliationsByObligation(obligationId: Long): Flow<List<Reconciliation>>
    fun getReconciliationsByEvidence(evidenceId: Long): Flow<List<Reconciliation>>
    fun getReconciliationById(id: Long): Flow<Reconciliation?>
    suspend fun getReconciliationByIdDirect(id: Long): Reconciliation?
    suspend fun getReconciliationsByEvidenceDirect(evidenceId: Long): List<Reconciliation>
    suspend fun getAllReconciliationsDirect(): List<Reconciliation>
    suspend fun saveReconciliation(reconciliation: Reconciliation): Long
    suspend fun deleteReconciliation(reconciliation: Reconciliation)
    fun getReconciliationCount(): Flow<Int>
    fun getTotalSettledAmount(): Flow<Money>
}

class ReconciliationRepositoryImpl(
    private val reconciliationDao: ReconciliationDao
) : ReconciliationRepository {

    override fun getAllReconciliations(): Flow<List<Reconciliation>> =
        reconciliationDao.getAllReconciliations().map { list -> list.map { it.toDomain() } }

    override fun getReconciliationsByObligation(obligationId: Long): Flow<List<Reconciliation>> =
        reconciliationDao.getReconciliationsByObligation(obligationId).map { list -> list.map { it.toDomain() } }

    override fun getReconciliationsByEvidence(evidenceId: Long): Flow<List<Reconciliation>> =
        reconciliationDao.getReconciliationsByEvidence(evidenceId).map { list -> list.map { it.toDomain() } }

    override fun getReconciliationById(id: Long): Flow<Reconciliation?> =
        reconciliationDao.getReconciliationById(id).map { it?.toDomain() }

    override suspend fun getReconciliationByIdDirect(id: Long): Reconciliation? =
        reconciliationDao.getReconciliationByIdDirect(id)?.toDomain()

    override suspend fun getReconciliationsByEvidenceDirect(evidenceId: Long): List<Reconciliation> =
        reconciliationDao.getReconciliationsByEvidenceDirect(evidenceId).map { it.toDomain() }

    override suspend fun getAllReconciliationsDirect(): List<Reconciliation> =
        reconciliationDao.getAllReconciliationsDirect().map { it.toDomain() }

    override suspend fun saveReconciliation(reconciliation: Reconciliation): Long =
        reconciliationDao.insertReconciliation(ReconciliationEntity.fromDomain(reconciliation))

    override suspend fun deleteReconciliation(reconciliation: Reconciliation) =
        reconciliationDao.deleteReconciliation(ReconciliationEntity.fromDomain(reconciliation))

    override fun getReconciliationCount(): Flow<Int> =
        reconciliationDao.getReconciliationCount()

    override fun getTotalSettledAmount(): Flow<Money> =
        reconciliationDao.getTotalSettledAmountPaise().map { Money.fromPaise(it ?: 0L) }
}

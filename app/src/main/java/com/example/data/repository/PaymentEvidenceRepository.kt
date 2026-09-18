package com.example.data.repository

import com.example.data.local.dao.PaymentEvidenceDao
import com.example.data.local.entities.PaymentEvidenceEntity
import com.example.domain.model.PaymentEvidence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface PaymentEvidenceRepository {
    fun getAllEvidence(): Flow<List<PaymentEvidence>>
    fun getEvidenceById(id: Long): Flow<PaymentEvidence?>
    suspend fun getEvidenceByIdDirect(id: Long): PaymentEvidence?
    suspend fun getEvidenceByUtr(utrNumber: String): PaymentEvidence?
    suspend fun saveEvidence(evidence: PaymentEvidence): Long
    suspend fun deleteEvidence(evidence: PaymentEvidence)
    fun getEvidenceCount(): Flow<Int>
    suspend fun updateReconciliationStatus(evidenceId: Long, isReconciled: Boolean)
    fun getUnreconciledEvidence(): Flow<List<PaymentEvidence>>
    suspend fun getUnreconciledEvidenceDirect(): List<PaymentEvidence>
}

class PaymentEvidenceRepositoryImpl(
    private val paymentEvidenceDao: PaymentEvidenceDao
) : PaymentEvidenceRepository {

    override fun getAllEvidence(): Flow<List<PaymentEvidence>> =
        paymentEvidenceDao.getAllEvidence().map { list -> list.map { it.toDomain() } }

    override fun getEvidenceById(id: Long): Flow<PaymentEvidence?> =
        paymentEvidenceDao.getEvidenceById(id).map { it?.toDomain() }

    override suspend fun getEvidenceByIdDirect(id: Long): PaymentEvidence? =
        paymentEvidenceDao.getEvidenceByIdDirect(id)?.toDomain()

    override suspend fun getEvidenceByUtr(utrNumber: String): PaymentEvidence? =
        paymentEvidenceDao.getEvidenceByUtr(utrNumber)?.toDomain()

    override suspend fun saveEvidence(evidence: PaymentEvidence): Long =
        paymentEvidenceDao.insertEvidence(PaymentEvidenceEntity.fromDomain(evidence))

    override suspend fun deleteEvidence(evidence: PaymentEvidence) =
        paymentEvidenceDao.deleteEvidence(PaymentEvidenceEntity.fromDomain(evidence))

    override fun getEvidenceCount(): Flow<Int> =
        paymentEvidenceDao.getEvidenceCount()

    override suspend fun updateReconciliationStatus(evidenceId: Long, isReconciled: Boolean) =
        paymentEvidenceDao.updateReconciliationStatus(evidenceId, isReconciled)

    override fun getUnreconciledEvidence(): Flow<List<PaymentEvidence>> =
        paymentEvidenceDao.getUnreconciledEvidence().map { list -> list.map { it.toDomain() } }

    override suspend fun getUnreconciledEvidenceDirect(): List<PaymentEvidence> =
        paymentEvidenceDao.getUnreconciledEvidenceDirect().map { it.toDomain() }
}

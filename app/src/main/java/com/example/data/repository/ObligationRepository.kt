package com.example.data.repository

import com.example.data.local.dao.ObligationDao
import com.example.data.local.entities.ObligationEntity
import com.example.domain.model.Money
import com.example.domain.model.Obligation
import com.example.domain.model.ObligationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ObligationRepository {
    fun getAllObligations(): Flow<List<Obligation>>
    fun getObligationsByCustomer(customerId: Long): Flow<List<Obligation>>
    suspend fun getObligationsByCustomerDirect(customerId: Long): List<Obligation>
    fun getOpenObligations(): Flow<List<Obligation>>
    suspend fun getOpenObligationsDirect(): List<Obligation>
    fun getObligationById(id: Long): Flow<Obligation?>
    suspend fun getObligationByIdDirect(id: Long): Obligation?
    suspend fun createObligation(obligation: Obligation): Long
    suspend fun updateObligation(obligation: Obligation)
    suspend fun updateSettlement(obligationId: Long, remainingAmount: Money, status: ObligationStatus)
    suspend fun deleteObligation(obligation: Obligation)
    fun getTotalOutstanding(): Flow<Money>
    fun getOpenObligationCount(): Flow<Int>
}

class ObligationRepositoryImpl(
    private val obligationDao: ObligationDao
) : ObligationRepository {

    override fun getAllObligations(): Flow<List<Obligation>> =
        obligationDao.getAllObligations().map { list -> list.map { it.toDomain() } }

    override fun getObligationsByCustomer(customerId: Long): Flow<List<Obligation>> =
        obligationDao.getObligationsByCustomer(customerId).map { list -> list.map { it.toDomain() } }

    override suspend fun getObligationsByCustomerDirect(customerId: Long): List<Obligation> =
        obligationDao.getObligationsByCustomerDirect(customerId).map { it.toDomain() }

    override fun getOpenObligations(): Flow<List<Obligation>> =
        obligationDao.getOpenObligations().map { list -> list.map { it.toDomain() } }

    override suspend fun getOpenObligationsDirect(): List<Obligation> =
        obligationDao.getOpenObligationsDirect().map { it.toDomain() }

    override fun getObligationById(id: Long): Flow<Obligation?> =
        obligationDao.getObligationById(id).map { it?.toDomain() }

    override suspend fun getObligationByIdDirect(id: Long): Obligation? =
        obligationDao.getObligationByIdDirect(id)?.toDomain()

    override suspend fun createObligation(obligation: Obligation): Long =
        obligationDao.insertObligation(ObligationEntity.fromDomain(obligation))

    override suspend fun updateObligation(obligation: Obligation) =
        obligationDao.updateObligation(ObligationEntity.fromDomain(obligation))

    override suspend fun updateSettlement(obligationId: Long, remainingAmount: Money, status: ObligationStatus) =
        obligationDao.updateObligationSettlement(obligationId, remainingAmount.paise, status)

    override suspend fun deleteObligation(obligation: Obligation) =
        obligationDao.deleteObligation(ObligationEntity.fromDomain(obligation))

    override fun getTotalOutstanding(): Flow<Money> =
        obligationDao.getTotalOutstandingPaise().map { Money.fromPaise(it ?: 0L) }

    override fun getOpenObligationCount(): Flow<Int> =
        obligationDao.getOpenObligationCount()
}

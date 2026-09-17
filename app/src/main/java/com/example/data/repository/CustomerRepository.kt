package com.example.data.repository

import com.example.data.local.dao.CustomerDao
import com.example.data.local.entities.CustomerEntity
import com.example.domain.model.Customer
import com.example.domain.model.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface CustomerRepository {
    fun getAllCustomers(): Flow<List<Customer>>
    fun getCustomerById(id: Long): Flow<Customer?>
    suspend fun getCustomerByIdDirect(id: Long): Customer?
    suspend fun getCustomerByNormalizedName(normalizedName: String): Customer?
    fun searchCustomers(query: String): Flow<List<Customer>>
    suspend fun saveCustomer(customer: Customer): Long
    suspend fun updateBalance(customerId: Long, newBalance: Money)
    suspend fun deleteCustomer(customer: Customer)
    fun getCustomerCount(): Flow<Int>
}

class CustomerRepositoryImpl(
    private val customerDao: CustomerDao
) : CustomerRepository {

    override fun getAllCustomers(): Flow<List<Customer>> =
        customerDao.getAllCustomers().map { list -> list.map { it.toDomain() } }

    override fun getCustomerById(id: Long): Flow<Customer?> =
        customerDao.getCustomerById(id).map { it?.toDomain() }

    override suspend fun getCustomerByIdDirect(id: Long): Customer? =
        customerDao.getCustomerByIdDirect(id)?.toDomain()

    override suspend fun getCustomerByNormalizedName(normalizedName: String): Customer? =
        customerDao.getCustomerByNormalizedName(normalizedName)?.toDomain()

    override fun searchCustomers(query: String): Flow<List<Customer>> =
        customerDao.searchCustomers(query).map { list -> list.map { it.toDomain() } }

    override fun getCustomerCount(): Flow<Int> =
        customerDao.getCustomerCount()

    override suspend fun saveCustomer(customer: Customer): Long {
        val normalized = customer.normalizedName.ifBlank {
            customer.name.trim().lowercase()
        }
        val entity = CustomerEntity.fromDomain(customer.copy(normalizedName = normalized))
        return customerDao.insertCustomer(entity)
    }

    override suspend fun updateBalance(customerId: Long, newBalance: Money) {
        customerDao.updateCustomerBalance(customerId, newBalance.paise)
    }

    override suspend fun deleteCustomer(customer: Customer) {
        customerDao.deleteCustomer(CustomerEntity.fromDomain(customer))
    }
}

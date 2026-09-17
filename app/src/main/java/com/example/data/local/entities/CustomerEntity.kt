package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Customer
import com.example.domain.model.Money

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val phoneNumber: String? = null,
    val currentBalancePaise: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Customer = Customer(
        id = id,
        name = name,
        normalizedName = normalizedName,
        phoneNumber = phoneNumber,
        currentBalance = Money.fromPaise(currentBalancePaise),
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(customer: Customer): CustomerEntity = CustomerEntity(
            id = customer.id,
            name = customer.name,
            normalizedName = customer.normalizedName,
            phoneNumber = customer.phoneNumber,
            currentBalancePaise = customer.currentBalance.paise,
            updatedAt = customer.updatedAt
        )
    }
}

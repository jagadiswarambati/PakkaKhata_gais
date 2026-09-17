package com.example.data.local.converters

import androidx.room.TypeConverter
import com.example.domain.model.ObligationStatus
import com.example.domain.model.SettlementOutcome

class DatabaseConverters {

    @TypeConverter
    fun fromObligationStatus(status: ObligationStatus?): String? = status?.name

    @TypeConverter
    fun toObligationStatus(value: String?): ObligationStatus? =
        value?.let { runCatching { ObligationStatus.valueOf(it) }.getOrDefault(ObligationStatus.OPEN) }

    @TypeConverter
    fun fromSettlementOutcome(outcome: SettlementOutcome?): String? = outcome?.name

    @TypeConverter
    fun toSettlementOutcome(value: String?): SettlementOutcome? =
        value?.let { runCatching { SettlementOutcome.valueOf(it) }.getOrDefault(SettlementOutcome.NO_MATCH) }
}

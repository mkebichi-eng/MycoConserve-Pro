package com.example.data

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Convertisseurs Room (@TypeConverter) pour persister BigDecimal en SQLite
 * avec une précision financière stricte au centime de Dinar Algérien (DZD).
 */
object BigDecimalConverters {
    @TypeConverter
    @JvmStatic
    fun fromBigDecimalToString(value: BigDecimal?): String {
        return (value ?: BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP)
            .toPlainString()
    }

    @TypeConverter
    @JvmStatic
    fun fromStringToBigDecimal(value: String?): BigDecimal {
        if (value.isNullOrBlank()) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        return try {
            BigDecimal(value.trim()).setScale(2, RoundingMode.HALF_UP)
        } catch (_: NumberFormatException) {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }
    }

    fun toCentimesLong(value: BigDecimal): Long =
        value.setScale(2, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .toLong()

    fun fromCentimesLong(centimes: Long): BigDecimal =
        BigDecimal.valueOf(centimes, 2)
}

fun Double.toDzdBigDecimal(): BigDecimal =
    BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP)

fun Int.toDzdBigDecimal(): BigDecimal =
    BigDecimal.valueOf(this.toLong()).setScale(2, RoundingMode.HALF_UP)

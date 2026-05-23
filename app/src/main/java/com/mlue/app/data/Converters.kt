package com.mlue.app.data

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? = time?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }

    @TypeConverter
    fun fromIntList(values: List<Int>?): String? = values?.joinToString(",")

    @TypeConverter
    fun toIntList(value: String?): List<Int> = value
        ?.split(",")
        ?.mapNotNull { it.toIntOrNull() }
        ?.toList()
        ?: emptyList()
}

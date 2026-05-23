package com.mlue.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "habit_completions",
    indices = [Index(value = ["habitId", "completionDate"], unique = true)]
)
data class HabitCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val completionDate: LocalDate
)

data class DateCount(
    val date: String,
    val count: Int
)

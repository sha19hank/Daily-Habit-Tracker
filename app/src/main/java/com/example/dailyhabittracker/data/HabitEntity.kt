package com.example.dailyhabittracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val color: Int = 0,
    val scheduledDays: List<Int> = emptyList(),
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime? = null,
    val paused: Boolean = false,
    val stepEnabled: Boolean = false,
    val stepGoal: Int? = null,
    val createdDate: LocalDate = LocalDate.now(),
    val lastCompletedDate: LocalDate? = null,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

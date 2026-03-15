package com.example.dailyhabittracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val goalId: Long = 0,
    val title: String,
    val description: String? = null,
    val startDate: LocalDate,
    val deadline: LocalDate? = null
)

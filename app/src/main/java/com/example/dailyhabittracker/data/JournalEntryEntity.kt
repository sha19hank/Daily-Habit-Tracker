package com.example.dailyhabittracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "journal_entries",
    indices = [Index(value = ["date"])]
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val date: LocalDate,
    val mood: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

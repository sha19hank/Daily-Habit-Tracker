package com.example.dailyhabittracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tokens")
data class TokenEntity(
    @PrimaryKey val id: Int = 1,
    val count: Int = 0,
    val lastEarnedDate: String? = null
)

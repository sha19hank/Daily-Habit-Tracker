package com.mlue.app.data

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
    val goalId: Long? = null,
    val createdDate: LocalDate = LocalDate.now(),
    val lastCompletedDate: LocalDate? = null,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val highestCelebratedMilestone: Int = 0
) {
    /**
     * Canonical temporal evaluation source.
     * All historical calculations MUST derive from these methods.
     * Future archival/versioning systems should integrate here.
     * 
     * NOTE ON FUTURE SCHEDULE VERSIONING:
     * Current temporal system correctly handles creation-date boundaries and paused states, 
     * but future habit schedule editing/versioning will eventually require historical schedule snapshots 
     * or schedule version history to prevent retroactive schedule mutation when a user changes their days.
     */
    fun isActiveOn(date: LocalDate): Boolean {
        if (paused) return false
        if (createdDate.isAfter(date)) return false
        return true
    }

    /**
     * Checks if the habit is scheduled to occur on the given date, respecting creation boundaries.
     */
    fun isScheduledOn(date: LocalDate): Boolean {
        if (!isActiveOn(date)) return false
        return scheduledDays.isEmpty() || scheduledDays.contains(date.dayOfWeek.value)
    }
}

package com.mlue.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdDate DESC")
    fun getHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits")
    suspend fun getHabitsOnce(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE goalId = :goalId ORDER BY createdDate DESC")
    suspend fun getHabitsByGoalId(goalId: Long): List<HabitEntity>

    @Query("UPDATE habits SET goalId = null WHERE goalId = :goalId")
    suspend fun clearGoalIdForHabits(goalId: Long)

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Long): HabitEntity?

    @Query("SELECT * FROM tokens WHERE id = 1")
    fun getTokenFlow(): Flow<TokenEntity?>

    @Query("SELECT * FROM tokens WHERE id = 1")
    suspend fun getTokenOnce(): TokenEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("UPDATE habits SET highestCelebratedMilestone = :milestone WHERE id = :habitId")
    suspend fun updateHighestCelebratedMilestone(habitId: Long, milestone: Int)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertToken(token: TokenEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: HabitCompletionEntity): Long

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND completionDate = :date")
    suspend fun deleteCompletionForDate(habitId: Long, date: String)

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY completionDate DESC")
    suspend fun getCompletionsForHabitDesc(habitId: Long): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions")
    fun getAllCompletionsFlow(): Flow<List<HabitCompletionEntity>>

    @Query("SELECT habitId FROM habit_completions WHERE completionDate = :date")
    suspend fun getCompletedHabitIdsForDate(date: String): List<Long>

    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId AND completionDate = :date")
    suspend fun hasCompletionForDate(habitId: Long, date: String): Int

    @Query(
        "SELECT completionDate as date, COUNT(*) as count " +
            "FROM habit_completions " +
            "WHERE completionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY completionDate ORDER BY completionDate"
    )
    suspend fun getCompletionCounts(startDate: String, endDate: String): List<DateCount>

    @Query("SELECT COUNT(*) FROM habit_completions WHERE completionDate BETWEEN :startDate AND :endDate")
    suspend fun getCompletionCountInRange(startDate: String, endDate: String): Int

    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId AND completionDate BETWEEN :startDate AND :endDate")
    suspend fun getCompletionCountForHabitInRange(habitId: Long, startDate: String, endDate: String): Int
}

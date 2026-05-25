package com.mlue.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY isCompleted ASC, startDate DESC")
    fun getGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY isCompleted ASC, startDate DESC")
    suspend fun getGoalsOnce(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE goalId = :goalId")
    suspend fun getGoalById(goalId: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("UPDATE goals SET isCompleted = :completed, completedDate = :date WHERE goalId = :goalId")
    suspend fun markGoalCompleted(goalId: Long, completed: Boolean, date: String?)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)
}

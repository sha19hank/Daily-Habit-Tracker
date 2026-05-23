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
    @Query("SELECT * FROM goals ORDER BY startDate DESC")
    fun getGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY startDate DESC")
    suspend fun getGoalsOnce(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE goalId = :goalId")
    suspend fun getGoalById(goalId: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)
}

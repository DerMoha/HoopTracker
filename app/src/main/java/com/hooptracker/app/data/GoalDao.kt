package com.hooptracker.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: Goal): Long

    @Update
    suspend fun update(goal: Goal)

    @Query("SELECT * FROM goals WHERE date = :date LIMIT 1")
    suspend fun getGoalForDate(date: Long): Goal?

    @Query("SELECT * FROM goals ORDER BY date DESC")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteById(goalId: Long)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}

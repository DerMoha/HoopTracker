package com.hooptracker.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long = getTodayStart(),
    val targetShots: Int = 100,
    val targetPercentage: Float = 50f
) {
    companion object {
        fun getTodayStart(): Long {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }
    }
}

data class GoalProgress(
    val goal: Goal,
    val currentShots: Int,
    val currentPercentage: Float,
    val shotsProgress: Float, // 0-100%
    val percentageProgress: Float // 0-100%
) {
    val isComplete: Boolean
        get() = shotsProgress >= 100f && currentPercentage >= goal.targetPercentage
}

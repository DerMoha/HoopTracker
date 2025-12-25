package com.hooptracker.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val notes: String? = null,
    val isActive: Boolean = true
) {
    fun getStartDate(): Date = Date(startTime)
    fun getEndDate(): Date? = endTime?.let { Date(it) }
    fun getDurationMinutes(): Int? {
        return endTime?.let {
            ((it - startTime) / 1000 / 60).toInt()
        }
    }
}

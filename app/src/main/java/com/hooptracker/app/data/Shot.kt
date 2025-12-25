package com.hooptracker.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "shots")
data class Shot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isHit: Boolean,
    val sessionId: String? = null
) {
    fun getDate(): Date = Date(timestamp)
}

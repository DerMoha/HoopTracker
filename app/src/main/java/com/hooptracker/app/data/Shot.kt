package com.hooptracker.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

enum class ShotType {
    GENERAL,
    THREE_POINTER,
    MID_RANGE,
    LAYUP,
    FREE_THROW
}

@Entity(tableName = "shots")
data class Shot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isHit: Boolean,
    val sessionId: Long? = null,
    val shotType: String = ShotType.GENERAL.name
) {
    fun getDate(): Date = Date(timestamp)
    fun getShotType(): ShotType = ShotType.valueOf(shotType)
}

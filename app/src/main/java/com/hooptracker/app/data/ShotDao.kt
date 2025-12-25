package com.hooptracker.app.data

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShotDao {
    @Insert
    suspend fun insert(shot: Shot): Long

    @Query("SELECT * FROM shots ORDER BY timestamp DESC")
    fun getAllShots(): Flow<List<Shot>>

    @Query("SELECT * FROM shots WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getShotsBetween(startTime: Long, endTime: Long): Flow<List<Shot>>

    @Query("SELECT COUNT(*) FROM shots WHERE isHit = 1 AND timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getHitsCount(startTime: Long, endTime: Long): Int

    @Query("SELECT COUNT(*) FROM shots WHERE isHit = 0 AND timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getMissesCount(startTime: Long, endTime: Long): Int

    @Query("SELECT COUNT(*) FROM shots WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getTotalShotsCount(startTime: Long, endTime: Long): Int

    @Query("DELETE FROM shots")
    suspend fun deleteAll()

    @Query("DELETE FROM shots WHERE id = :shotId")
    suspend fun deleteById(shotId: Long)

    @Query("SELECT * FROM shots WHERE timestamp >= :startTime ORDER BY timestamp ASC")
    suspend fun getShotsFromTime(startTime: Long): List<Shot>

    @Query("SELECT * FROM shots WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    suspend fun getShotsBySession(sessionId: Long): List<Shot>

    @Query("SELECT * FROM shots WHERE shotType = :shotType AND timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getShotsByType(shotType: String, startTime: Long, endTime: Long): List<Shot>

    @Query("SELECT COUNT(*) FROM shots WHERE isHit = 1 AND shotType = :shotType AND timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getHitsCountByType(shotType: String, startTime: Long, endTime: Long): Int

    @Query("SELECT COUNT(*) FROM shots WHERE shotType = :shotType AND timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getTotalShotsCountByType(shotType: String, startTime: Long, endTime: Long): Int

    @Query("SELECT * FROM shots ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastShot(): Shot?

    @Query("SELECT * FROM shots ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentShots(limit: Int): List<Shot>
}

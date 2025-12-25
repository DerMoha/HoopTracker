package com.hooptracker.app.data

import kotlinx.coroutines.flow.Flow
import java.util.*

class ShotRepository(private val shotDao: ShotDao) {

    val allShots: Flow<List<Shot>> = shotDao.getAllShots()

    suspend fun insert(shot: Shot): Long {
        return shotDao.insert(shot)
    }

    suspend fun recordHit() {
        insert(Shot(isHit = true))
    }

    suspend fun recordMiss() {
        insert(Shot(isHit = false))
    }

    suspend fun getStats(startTime: Long, endTime: Long): ShotStats {
        val hits = shotDao.getHitsCount(startTime, endTime)
        val misses = shotDao.getMissesCount(startTime, endTime)
        val total = hits + misses
        val percentage = if (total > 0) (hits.toFloat() / total.toFloat() * 100) else 0f

        return ShotStats(
            hits = hits,
            misses = misses,
            total = total,
            percentage = percentage
        )
    }

    suspend fun getTodayStats(): ShotStats {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        val endOfDay = System.currentTimeMillis()

        return getStats(startOfDay, endOfDay)
    }

    suspend fun getWeeklyStats(): ShotStats {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.timeInMillis
        val endOfWeek = System.currentTimeMillis()

        return getStats(startOfWeek, endOfWeek)
    }

    suspend fun getMonthlyStats(): ShotStats {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis
        val endOfMonth = System.currentTimeMillis()

        return getStats(startOfMonth, endOfMonth)
    }

    suspend fun getYearlyStats(): ShotStats {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfYear = calendar.timeInMillis
        val endOfYear = System.currentTimeMillis()

        return getStats(startOfYear, endOfYear)
    }

    suspend fun getDailyStatsForPeriod(days: Int): List<DailyStats> {
        val result = mutableListOf<DailyStats>()
        val calendar = Calendar.getInstance()

        for (i in days - 1 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfDay = calendar.timeInMillis

            val stats = getStats(startOfDay, endOfDay)
            result.add(DailyStats(Date(startOfDay), stats))
        }

        return result
    }

    suspend fun deleteAll() {
        shotDao.deleteAll()
    }
}

data class ShotStats(
    val hits: Int,
    val misses: Int,
    val total: Int,
    val percentage: Float
)

data class DailyStats(
    val date: Date,
    val stats: ShotStats
)

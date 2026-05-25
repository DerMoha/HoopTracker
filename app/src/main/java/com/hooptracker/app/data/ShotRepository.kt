package com.hooptracker.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ShotRepository(
    private val shotDao: ShotDao,
    private val sessionDao: SessionDao,
    private val goalDao: GoalDao,
    private val context: Context
) {

    private data class TimeRange(
        val startTime: Long,
        val endTime: Long
    )

    val allShots: Flow<List<Shot>> = shotDao.getAllShots()
    val allSessions: Flow<List<Session>> = sessionDao.getAllSessions()
    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()

    private var lastShotId: Long? = null

    // Shot operations
    suspend fun insert(shot: Shot): Long {
        val id = shotDao.insert(shot)
        lastShotId = id
        return id
    }

    suspend fun recordHit(sessionId: Long? = null, shotType: ShotType = ShotType.GENERAL) {
        insert(Shot(isHit = true, sessionId = sessionId, shotType = shotType.name))
    }

    suspend fun recordMiss(sessionId: Long? = null, shotType: ShotType = ShotType.GENERAL) {
        insert(Shot(isHit = false, sessionId = sessionId, shotType = shotType.name))
    }

    suspend fun undoLastShot(): Shot? {
        val lastShot = shotDao.getLastShot() ?: return null
        shotDao.deleteById(lastShot.id)
        if (lastShotId == lastShot.id) {
            lastShotId = null
        }
        return lastShot
    }

    suspend fun deleteShot(shotId: Long) {
        shotDao.deleteById(shotId)
    }

    suspend fun restoreShot(shot: Shot) {
        insert(shot.copy(id = 0))
    }

    suspend fun getLastShot(): Shot? = shotDao.getLastShot()

    suspend fun getRecentShots(limit: Int): List<Shot> = shotDao.getRecentShots(limit)

    // Statistics
    suspend fun getStats(startTime: Long, endTime: Long, shotType: ShotType? = null): ShotStats {
        val hits = if (shotType == null) {
            shotDao.getHitsCount(startTime, endTime)
        } else {
            shotDao.getHitsCountByType(shotType.name, startTime, endTime)
        }
        val total = if (shotType == null) {
            shotDao.getTotalShotsCount(startTime, endTime)
        } else {
            shotDao.getTotalShotsCountByType(shotType.name, startTime, endTime)
        }
        val misses = total - hits
        val percentage = if (total > 0) (hits.toFloat() / total.toFloat() * 100) else 0f

        return ShotStats(
            hits = hits,
            misses = misses,
            total = total,
            percentage = percentage
        )
    }

    suspend fun getTodayStats(shotType: ShotType? = null): ShotStats =
        todayRange().let { getStats(it.startTime, it.endTime, shotType) }

    suspend fun getWeeklyStats(shotType: ShotType? = null): ShotStats =
        currentWeekRange().let { getStats(it.startTime, it.endTime, shotType) }

    suspend fun getMonthlyStats(shotType: ShotType? = null): ShotStats =
        currentMonthRange().let { getStats(it.startTime, it.endTime, shotType) }

    suspend fun getYearlyStats(shotType: ShotType? = null): ShotStats =
        currentYearRange().let { getStats(it.startTime, it.endTime, shotType) }

    suspend fun getStatsForPeriod(period: StatsPeriod, shotType: ShotType? = null): ShotStats = when (period) {
        StatsPeriod.TODAY -> getTodayStats(shotType)
        StatsPeriod.WEEK -> getWeeklyStats(shotType)
        StatsPeriod.MONTH -> getMonthlyStats(shotType)
        StatsPeriod.YEAR -> getYearlyStats(shotType)
    }

    suspend fun getChartData(period: StatsPeriod, shotType: ShotType? = null): List<DailyStats> = when (period) {
        StatsPeriod.TODAY -> emptyList()
        StatsPeriod.WEEK -> getDailyStatsForPeriod(7, shotType)
        StatsPeriod.MONTH -> getDailyStatsForPeriod(30, shotType)
        StatsPeriod.YEAR -> getMonthlyStatsForYear(12, shotType)
    }

    suspend fun getDailyStatsForPeriod(days: Int, shotType: ShotType? = null): List<DailyStats> {
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

            val stats = getStats(startOfDay, endOfDay, shotType)
            result.add(DailyStats(Date(startOfDay), stats))
        }

        return result
    }

    suspend fun getMonthlyStatsForYear(months: Int, shotType: ShotType? = null): List<DailyStats> {
        val result = mutableListOf<DailyStats>()
        val calendar = Calendar.getInstance()

        for (i in months - 1 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.add(Calendar.MONTH, -i)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val endOfMonth = calendar.timeInMillis

            val stats = getStats(startOfMonth, endOfMonth, shotType)
            result.add(DailyStats(Date(startOfMonth), stats))
        }

        return result
    }

    // Stats by shot type
    suspend fun getStatsByType(shotType: ShotType, startTime: Long, endTime: Long): ShotStats {
        val hits = shotDao.getHitsCountByType(shotType.name, startTime, endTime)
        val total = shotDao.getTotalShotsCountByType(shotType.name, startTime, endTime)
        val misses = total - hits
        val percentage = if (total > 0) (hits.toFloat() / total.toFloat() * 100) else 0f

        return ShotStats(hits, misses, total, percentage)
    }

    suspend fun getAllTypeStats(startTime: Long, endTime: Long): Map<ShotType, ShotStats> {
        val result = mutableMapOf<ShotType, ShotStats>()
        ShotType.values().forEach { type ->
            result[type] = getStatsByType(type, startTime, endTime)
        }
        return result
    }

    // Streak tracking
    suspend fun getCurrentStreak(): StreakInfo {
        val recent = shotDao.getRecentShots(100)
        if (recent.isEmpty()) {
            return StreakInfo(0, true)
        }

        var streak = 0
        val isHitStreak = recent.first().isHit

        for (shot in recent) {
            if (shot.isHit == isHitStreak) {
                streak++
            } else {
                break
            }
        }

        return StreakInfo(streak, isHitStreak)
    }

    // Session management
    suspend fun startSession(): Long {
        val session = Session()
        return sessionDao.insert(session)
    }

    suspend fun endSession(sessionId: Long, notes: String? = null): Session? {
        val session = sessionDao.getSessionById(sessionId)
        return session?.let {
            val updated = it.copy(
                endTime = System.currentTimeMillis(),
                isActive = false,
                notes = notes
            )
            sessionDao.update(updated)
            updated
        }
    }

    suspend fun getActiveSession(): Session? = sessionDao.getActiveSession()

    suspend fun getSessionById(sessionId: Long): Session? = sessionDao.getSessionById(sessionId)

    suspend fun getShotsBySession(sessionId: Long): List<Shot> =
        shotDao.getShotsBySession(sessionId)

    suspend fun getSessionStats(sessionId: Long): SessionStats {
        val shots = getShotsBySession(sessionId)
        val hits = shots.count { it.isHit }
        val total = shots.size
        val misses = total - hits
        val percentage = if (total > 0) (hits.toFloat() / total.toFloat() * 100) else 0f

        val session = getSessionById(sessionId)
        return SessionStats(
            session = session,
            stats = ShotStats(hits, misses, total, percentage),
            totalShots = total
        )
    }

    // Goal management
    suspend fun getTodayGoal(): Goal {
        val today = Goal.getTodayStart()
        return goalDao.getGoalForDate(today) ?: Goal(date = today).also {
            goalDao.insert(it)
        }
    }

    suspend fun updateTodayGoal(targetShots: Int, targetPercentage: Float) {
        val goal = getTodayGoal()
        val updated = goal.copy(targetShots = targetShots, targetPercentage = targetPercentage)
        goalDao.update(updated)
    }

    suspend fun getTodayGoalProgress(): GoalProgress {
        val goal = getTodayGoal()
        val stats = getTodayStats()

        val shotsProgress = if (goal.targetShots > 0) {
            (stats.total.toFloat() / goal.targetShots.toFloat() * 100).coerceAtMost(100f)
        } else 100f

        val percentageProgress = if (goal.targetPercentage > 0 && stats.total > 0) {
            (stats.percentage / goal.targetPercentage * 100).coerceAtMost(100f)
        } else 0f

        return GoalProgress(
            goal = goal,
            currentShots = stats.total,
            currentPercentage = stats.percentage,
            shotsProgress = shotsProgress,
            percentageProgress = percentageProgress
        )
    }

    // Export to CSV
    suspend fun exportToCSV(): File {
        val shots = shotDao.getAllShotsList()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val csv = StringBuilder()
        csv.append("Timestamp,Date,Result,Shot Type,Session ID\n")

        shots.reversed().forEach { shot ->
            csv.append("${shot.timestamp},")
            csv.append("\"${dateFormat.format(shot.getDate())}\",")
            csv.append("${if (shot.isHit) "Hit" else "Miss"},")
            csv.append("${shot.getShotTypeEnum()},")
            csv.append("${shot.sessionId ?: ""}\n")
        }

        val fileName = "hooptracker_export_${System.currentTimeMillis()}.csv"
        val file = File(context.getExternalFilesDir(null), fileName)
        file.writeText(csv.toString())

        return file
    }

    // Clear data
    suspend fun deleteAll() {
        shotDao.deleteAll()
        sessionDao.deleteAll()
        goalDao.deleteAll()
        lastShotId = null
    }

    suspend fun deleteSession(sessionId: Long) {
        sessionDao.deleteById(sessionId)
    }

    private fun todayRange(): TimeRange {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return TimeRange(calendar.timeInMillis, System.currentTimeMillis())
    }

    private fun currentWeekRange(): TimeRange {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return TimeRange(calendar.timeInMillis, System.currentTimeMillis())
    }

    private fun currentMonthRange(): TimeRange {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return TimeRange(calendar.timeInMillis, System.currentTimeMillis())
    }

    private fun currentYearRange(): TimeRange {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return TimeRange(calendar.timeInMillis, System.currentTimeMillis())
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

data class StreakInfo(
    val count: Int,
    val isHitStreak: Boolean
)

data class SessionStats(
    val session: Session?,
    val stats: ShotStats,
    val totalShots: Int
)

package com.hooptracker.app.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.hooptracker.app.data.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(private val repository: ShotRepository) : ViewModel() {

    val allShots: LiveData<List<Shot>> = repository.allShots.asLiveData()
    val allSessions: LiveData<List<Session>> = repository.allSessions.asLiveData()

    private val _todayStats = MutableLiveData<ShotStats>()
    val todayStats: LiveData<ShotStats> = _todayStats

    private val _weeklyStats = MutableLiveData<ShotStats>()
    val weeklyStats: LiveData<ShotStats> = _weeklyStats

    private val _monthlyStats = MutableLiveData<ShotStats>()
    val monthlyStats: LiveData<ShotStats> = _monthlyStats

    private val _yearlyStats = MutableLiveData<ShotStats>()
    val yearlyStats: LiveData<ShotStats> = _yearlyStats

    private val _weeklyChartData = MutableLiveData<List<DailyStats>>()
    val weeklyChartData: LiveData<List<DailyStats>> = _weeklyChartData

    private val _monthlyChartData = MutableLiveData<List<DailyStats>>()
    val monthlyChartData: LiveData<List<DailyStats>> = _monthlyChartData

    private val _currentStreak = MutableLiveData<StreakInfo>()
    val currentStreak: LiveData<StreakInfo> = _currentStreak

    private val _goalProgress = MutableLiveData<GoalProgress>()
    val goalProgress: LiveData<GoalProgress> = _goalProgress

    private val _typeStats = MutableLiveData<Map<ShotType, ShotStats>>()
    val typeStats: LiveData<Map<ShotType, ShotStats>> = _typeStats

    private val _exportFile = MutableLiveData<File?>()
    val exportFile: LiveData<File?> = _exportFile

    init {
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            _todayStats.value = repository.getTodayStats()
            _weeklyStats.value = repository.getWeeklyStats()
            _monthlyStats.value = repository.getMonthlyStats()
            _yearlyStats.value = repository.getYearlyStats()
            _weeklyChartData.value = repository.getDailyStatsForPeriod(7)
            _monthlyChartData.value = repository.getDailyStatsForPeriod(30)
            _currentStreak.value = repository.getCurrentStreak()
            _goalProgress.value = repository.getTodayGoalProgress()

            // Get shot type stats for today
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            val endOfDay = System.currentTimeMillis()

            _typeStats.value = repository.getAllTypeStats(startOfDay, endOfDay)
        }
    }

    fun recordHit(sessionId: Long? = null, shotType: ShotType = ShotType.GENERAL) {
        viewModelScope.launch {
            repository.recordHit(sessionId, shotType)
            refreshStats()
        }
    }

    fun recordMiss(sessionId: Long? = null, shotType: ShotType = ShotType.GENERAL) {
        viewModelScope.launch {
            repository.recordMiss(sessionId, shotType)
            refreshStats()
        }
    }

    fun undoLastShot() {
        viewModelScope.launch {
            repository.undoLastShot()
            refreshStats()
        }
    }

    fun deleteShot(shotId: Long) {
        viewModelScope.launch {
            repository.deleteShot(shotId)
            refreshStats()
        }
    }

    fun startSession(): LiveData<Long> {
        val result = MutableLiveData<Long>()
        viewModelScope.launch {
            val sessionId = repository.startSession()
            result.postValue(sessionId)
        }
        return result
    }

    fun endSession(sessionId: Long, notes: String? = null) {
        viewModelScope.launch {
            repository.endSession(sessionId, notes)
            refreshStats()
        }
    }

    fun updateGoal(targetShots: Int, targetPercentage: Float) {
        viewModelScope.launch {
            repository.updateTodayGoal(targetShots, targetPercentage)
            _goalProgress.value = repository.getTodayGoalProgress()
        }
    }

    fun exportToCSV() {
        viewModelScope.launch {
            val file = repository.exportToCSV()
            _exportFile.postValue(file)
        }
    }

    fun clearExportFile() {
        _exportFile.value = null
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAll()
            refreshStats()
        }
    }
}

class MainViewModelFactory(private val repository: ShotRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

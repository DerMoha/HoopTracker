package com.hooptracker.app.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.hooptracker.app.data.DailyStats
import com.hooptracker.app.data.GoalProgress
import com.hooptracker.app.data.Shot
import com.hooptracker.app.data.ShotRepository
import com.hooptracker.app.data.ShotStats
import com.hooptracker.app.data.ShotType
import com.hooptracker.app.data.StatsPeriod
import com.hooptracker.app.data.StreakInfo
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(private val repository: ShotRepository) : ViewModel() {

    val allShots: LiveData<List<Shot>> = repository.allShots.asLiveData()

    private val _stats = MutableLiveData<ShotStats>()
    val stats: LiveData<ShotStats> = _stats

    private val _chartData = MutableLiveData<List<DailyStats>>(emptyList())
    val chartData: LiveData<List<DailyStats>> = _chartData

    private val _selectedPeriod = MutableLiveData(StatsPeriod.TODAY)
    val selectedPeriod: LiveData<StatsPeriod> = _selectedPeriod

    private val _statsFilterShotType = MutableLiveData<ShotType?>(null)
    val statsFilterShotType: LiveData<ShotType?> = _statsFilterShotType

    private val _currentStreak = MutableLiveData<StreakInfo>()
    val currentStreak: LiveData<StreakInfo> = _currentStreak

    private val _goalProgress = MutableLiveData<GoalProgress>()
    val goalProgress: LiveData<GoalProgress> = _goalProgress

    private val _exportFile = MutableLiveData<File?>()
    val exportFile: LiveData<File?> = _exportFile

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isExporting = MutableLiveData(false)
    val isExporting: LiveData<Boolean> = _isExporting

    fun clearError() {
        _error.value = null
    }

    private fun launchSafe(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                _error.postValue(e.message ?: "An unexpected error occurred")
            }
        }
    }

    init {
        refreshStats()
    }

    private suspend fun loadDashboardState() {
        val period = _selectedPeriod.value ?: StatsPeriod.TODAY
        val shotType = _statsFilterShotType.value
        _stats.value = repository.getStatsForPeriod(period, shotType)
        _chartData.value = repository.getChartData(period, shotType)
        _currentStreak.value = repository.getCurrentStreak()
        _goalProgress.value = repository.getTodayGoalProgress()
    }

    fun setSelectedPeriod(period: StatsPeriod) {
        if (_selectedPeriod.value == period) return
        _selectedPeriod.value = period
        refreshStats()
    }

    fun setStatsFilterShotType(shotType: ShotType?) {
        if (_statsFilterShotType.value == shotType) return
        _statsFilterShotType.value = shotType
        refreshStats()
    }

    fun refreshStats() {
        launchSafe {
            loadDashboardState()
        }
    }

    fun recordHit(sessionId: Long? = null, shotType: ShotType = ShotType.GENERAL) {
        launchSafe {
            repository.recordHit(sessionId, shotType)
            loadDashboardState()
        }
    }

    fun recordMiss(sessionId: Long? = null, shotType: ShotType = ShotType.GENERAL) {
        launchSafe {
            repository.recordMiss(sessionId, shotType)
            loadDashboardState()
        }
    }

    fun undoLastShot(onComplete: (Boolean) -> Unit = {}) {
        launchSafe {
            val deletedShot = repository.undoLastShot()
            loadDashboardState()
            onComplete(deletedShot != null)
        }
    }

    fun deleteShot(shotId: Long) {
        launchSafe {
            repository.deleteShot(shotId)
            loadDashboardState()
        }
    }

    fun restoreShot(shot: Shot) {
        launchSafe {
            repository.restoreShot(shot)
            loadDashboardState()
        }
    }

    fun startSession(onComplete: (Long) -> Unit) {
        launchSafe {
            onComplete(repository.startSession())
        }
    }

    fun endSession(sessionId: Long, notes: String? = null) {
        launchSafe {
            repository.endSession(sessionId, notes)
            loadDashboardState()
        }
    }

    fun updateGoal(targetShots: Int, targetPercentage: Float) {
        launchSafe {
            repository.updateTodayGoal(targetShots, targetPercentage)
            loadDashboardState()
        }
    }

    fun exportToCSV() {
        launchSafe {
            _isExporting.value = true
            val file = repository.exportToCSV()
            _exportFile.postValue(file)
            _isExporting.value = false
        }
    }

    fun clearExportFile() {
        _exportFile.value = null
    }

    fun clearAllData() {
        launchSafe {
            repository.deleteAll()
            loadDashboardState()
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

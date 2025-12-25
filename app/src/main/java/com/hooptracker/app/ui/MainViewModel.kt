package com.hooptracker.app.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.hooptracker.app.data.DailyStats
import com.hooptracker.app.data.Shot
import com.hooptracker.app.data.ShotRepository
import com.hooptracker.app.data.ShotStats
import kotlinx.coroutines.launch

class MainViewModel(private val repository: ShotRepository) : ViewModel() {

    val allShots: LiveData<List<Shot>> = repository.allShots.asLiveData()

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
        }
    }

    fun recordHit() {
        viewModelScope.launch {
            repository.recordHit()
            refreshStats()
        }
    }

    fun recordMiss() {
        viewModelScope.launch {
            repository.recordMiss()
            refreshStats()
        }
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

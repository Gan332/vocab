package com.vocabapp.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vocabapp.VocabApp
import com.vocabapp.data.db.entity.CheckinEntity
import com.vocabapp.data.db.entity.HistoryEntity
import com.vocabapp.data.repository.VocabRepository
import com.vocabapp.util.TimeUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StatsUiState(
    val sessionCount: Int = 0,
    val averageAccuracy: String = "0%",
    val totalDuration: String = "0m",
    val history: List<HistoryEntity> = emptyList(),
    val dailyGoal: Int = 20,
    val todayCount: Int = 0,
    val streak: Int = 0,
    val checkins: List<CheckinEntity> = emptyList(),
    val weeklyData: List<WeeklyBar> = emptyList()
)

data class WeeklyBar(val label: String, val count: Int, val isToday: Boolean)

class StatsViewModel(
    private val repository: VocabRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            val sessionCount = repository.getSessionCount()
            val avgAcc = repository.getAverageAccuracy()
            val totalDuration = repository.getTotalDuration()
            val todayCount = repository.getTodayCount()
            val checkins = repository.getAllCheckins()
            val streak = calcStreak(checkins)

            _uiState.update {
                it.copy(
                    sessionCount = sessionCount,
                    averageAccuracy = "${avgAcc.toInt()}%",
                    totalDuration = "${totalDuration / 60}m",
                    todayCount = todayCount,
                    streak = streak,
                    checkins = checkins,
                    weeklyData = buildWeeklyData(checkins)
                )
            }
        }

        // Observe history
        repository.getAllHistory().onEach { history ->
            _uiState.update { it.copy(history = history) }
        }.launchIn(viewModelScope)
    }

    fun updateDailyGoal(goal: Int) {
        _uiState.update { it.copy(dailyGoal = goal.coerceIn(1, 500)) }
    }

    private fun calcStreak(checkins: List<CheckinEntity>): Int {
        val goal = _uiState.value.dailyGoal
        val checkinMap = checkins.associate { it.date to it.count }
        var streak = 0
        for (i in 0 until 365) {
            val key = TimeUtils.getDateString(-i)
            val count = checkinMap[key] ?: 0
            if (count >= goal) {
                streak++
            } else if (i == 0) {
                continue
            } else {
                break
            }
        }
        return streak
    }

    private fun buildWeeklyData(checkins: List<CheckinEntity>): List<WeeklyBar> {
        val dayLabels = listOf("日", "一", "二", "三", "四", "五", "六")
        val checkinMap = checkins.associate { it.date to it.count }
        val result = mutableListOf<WeeklyBar>()
        for (i in 6 downTo 0) {
            val key = TimeUtils.getDateString(-i)
            val count = checkinMap[key] ?: 0
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
            val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
            result.add(WeeklyBar(dayLabels[dayOfWeek], count, i == 0))
        }
        return result
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StatsViewModel(VocabApp.instance.repository) as T
            }
        }
    }
}

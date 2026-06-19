package com.example.loveosapk.ui.features.cycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveosapk.data.PreferenceManager
import com.example.loveosapk.domain.model.CycleLog
import com.example.loveosapk.domain.repository.CycleRepository
import com.example.loveosapk.domain.usecase.GetCycleMonthUseCase
import com.example.loveosapk.domain.usecase.SaveCycleLogUseCase
import com.example.loveosapk.ui.features.cycle.model.CycleUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import androidx.lifecycle.ViewModelProvider
import com.example.loveosapk.domain.usecase.PredictCyclePhasesUseCase
import com.example.loveosapk.ui.MainViewModel
import kotlinx.coroutines.CancellationException

class CycleViewModel(
    private val getCycleMonth: GetCycleMonthUseCase,
    private val saveLog: SaveCycleLogUseCase,
    private val repository: CycleRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    class Factory(private val mainViewModel: MainViewModel) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = mainViewModel.cycleRepository
            val predictPhases = PredictCyclePhasesUseCase()
            val getCycleMonth = GetCycleMonthUseCase(repository, predictPhases)
            val saveLog = SaveCycleLogUseCase(repository)
            
            return CycleViewModel(getCycleMonth, saveLog, repository, mainViewModel.preferenceManager) as T
        }
    }

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    private val appState = preferenceManager.appStateFlow

    val uiState: StateFlow<CycleUiState> = combine(
        _currentMonth,
        _selectedDate,
        appState,
        repository.getCycleStats()
    ) { month, selected, state, stats ->
        android.util.Log.d("CYCLE_DIAGNOSTIC", "Combine triggered: month=$month, selected=$selected")
        Triple(month, selected, stats)
    }.flatMapLatest { (month, selected, stats) ->
        getCycleMonth(month).map { days ->
            android.util.Log.d("CYCLE_DIAGNOSTIC", "Mapping days: count=${days.size}")
            CycleUiState.Success(
                currentMonth = month,
                days = days.map { it.copy(isSelected = it.date == selected) },
                selectedDate = selected,
                cycleStats = stats,
                partnerLogs = days.filter { day -> 
                    day.log?.source == "partner" && (day.log.symptoms.isNotEmpty() || day.log.notes.isNotBlank())
                }
            ) as CycleUiState
        }
    }.catch { e ->
        android.util.Log.e("CYCLE_DIAGNOSTIC", "Error in uiState flow", e)
        emit(CycleUiState.Error(e.message ?: "Unknown error"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CycleUiState.Loading
    )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = if (_selectedDate.value == date) null else date
    }

    fun changeMonth(month: YearMonth) {
        _currentMonth.value = month
        _selectedDate.value = null
    }

    fun saveCycleLog(log: CycleLog) {
        viewModelScope.launch {
            try {
                saveLog(log.copy(source = "me"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("CYCLE_DIAGNOSTIC", "Failed to save cycle log", e)
            }
        }
    }

    fun deleteLog(date: LocalDate) {
        viewModelScope.launch {
            repository.deleteLog(date)
            if (_selectedDate.value == date) _selectedDate.value = null
        }
    }
}

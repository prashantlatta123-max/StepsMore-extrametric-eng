
package com.sarotech.stepsmore.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sarotech.stepsmore.data.StepsRepository
import com.sarotech.stepsmore.data.db.DailyStepsEntity
import com.sarotech.stepsmore.data.db.HourlyStepsEntity
import com.sarotech.stepsmore.data.prefs.UserSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class StepsViewModel(private val repository: StepsRepository) : ViewModel() {

    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    val settings: StateFlow<UserSettings> = repository.prefs.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val todaySteps: StateFlow<Int> = _currentDate
        .flatMapLatest { date ->
            repository.observeStepsForDate(
                date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            )
        }
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val past7Days: StateFlow<List<DailyStepsEntity>> = _currentDate
        .flatMapLatest { date -> repository.observePast7Days(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDaily: StateFlow<List<DailyStepsEntity>> =
        repository.observeAllDaily()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDateIso = MutableStateFlow<String?>(null)
    val selectedDateIso: StateFlow<String?> = _selectedDateIso.asStateFlow()

    val selectedHourlySteps: StateFlow<List<HourlyStepsEntity>> =
        _selectedDateIso
            .flatMapLatest { date ->
                if (date == null) flowOf(emptyList())
                else repository.observeHourlyForDate(date)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshDate() {
        val now = LocalDate.now()
        if (_currentDate.value != now) {
            _currentDate.value = now
        }
    }

    fun toggleSelectedDate(dateIso: String) {
        _selectedDateIso.value =
            if (_selectedDateIso.value == dateIso) null else dateIso
    }

    fun clearSelectedDate() {
        _selectedDateIso.value = null
    }

    fun togglePause() {
        viewModelScope.launch {
            val current = settings.value.isPaused
            repository.prefs.setPaused(!current, 0L)
        }
    }

    fun setTimedPause(minutes: Int) {
        viewModelScope.launch {
            val until = System.currentTimeMillis() + minutes * 60_000L
            repository.prefs.setPaused(true, until)
        }
    }

    fun updateDailyGoal(goal: Int) =
        viewModelScope.launch {
            repository.prefs.setDailyGoal(goal)
        }

    fun setNotifyWhenReached(v: Boolean) =
        viewModelScope.launch {
            repository.prefs.setNotifyWhenReached(v)
        }

    fun setShowProgressInNotification(v: Boolean) =
        viewModelScope.launch {
            repository.prefs.setShowProgressInNotification(v)
        }

    fun setEncouragingNotifications(v: Boolean) =
        viewModelScope.launch {
            repository.prefs.setEncouragingNotifications(v)
        }

    fun setDrawTargetLine(v: Boolean) =
        viewModelScope.launch {
            repository.prefs.setDrawTargetLine(v)
        }

    fun setHeightCm(v: Int) =
        viewModelScope.launch {
            repository.prefs.setHeightCm(v)
        }

    fun setStepLengthCm(v: Float) =
        viewModelScope.launch {
            repository.prefs.setStepLengthCm(v)
        }

    fun setWeightKg(v: Int) =
        viewModelScope.launch {
            repository.prefs.setWeightKg(v)
        }

    fun setUnitSystem(v: String) =
        viewModelScope.launch {
            repository.prefs.setUnitSystem(v)
        }

    fun setDateFormat(v: String) =
        viewModelScope.launch {
            repository.prefs.setDateFormat(v)
        }

    fun setFirstDayOfWeek(v: String) =
        viewModelScope.launch {
            repository.prefs.setFirstDayOfWeek(v)
        }

    fun setSensorMode(v: String) =
        viewModelScope.launch {
            repository.prefs.setSensorMode(v)
        }

    fun setThemeMode(v: String) =
        viewModelScope.launch {
            repository.prefs.setThemeMode(v)
        }

    // Custom Accent Color
    fun setAccentColorHex(v: String) =
        viewModelScope.launch {
            repository.prefs.setAccentColorHex(v)
        }

    suspend fun exportBackupJson(): String =
        repository.exportBackupJson()

    suspend fun importBackupJson(json: String): Int =
        repository.importBackupJson(json)

    class Factory(
        private val repository: StepsRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T {
            return StepsViewModel(repository) as T
        }
    }
}

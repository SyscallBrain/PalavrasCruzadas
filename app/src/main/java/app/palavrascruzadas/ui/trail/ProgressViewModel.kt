package app.palavrascruzadas.ui.trail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.palavrascruzadas.data.ProgressData
import app.palavrascruzadas.data.ProgressRepository
import app.palavrascruzadas.data.StatsData
import app.palavrascruzadas.data.StatsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Progress shared by the home, trail and game screens (source: the progress store). */
class ProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProgressRepository.from(application)

    val stats: StateFlow<StatsData> = StatsRepository.from(application).data.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        StatsData(),
    )

    val data: StateFlow<ProgressData> = repository.data.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ProgressData(),
    )

    suspend fun markSeen(type: String) = repository.markSeen(type)

    suspend fun setSimulatedDay(day: Long?) = repository.setSimulatedDay(day)
}

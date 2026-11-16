package com.passion.knownow.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.passion.knownow.data.ServiceLocator
import com.passion.knownow.data.db.DownloadState
import com.passion.knownow.data.db.DownloadTaskEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val tasks: List<DownloadTaskEntity> = emptyList(),
) {
    val active: List<DownloadTaskEntity> get() = tasks.filter { it.state in DownloadState.active }
    val finished: List<DownloadTaskEntity> get() = tasks.filter { it.state !in DownloadState.active }
    val completedUnits: Int get() = active.sumOf { it.completed }
    val totalUnits: Int get() = active.sumOf { it.total }
    val fraction: Float
        get() = if (totalUnits > 0) (completedUnits.toFloat() / totalUnits).coerceIn(0f, 1f) else 0f
    val isRunning: Boolean get() = active.any { it.state == DownloadState.RUNNING }
}

class DownloadsViewModel : ViewModel() {
    private val repo = ServiceLocator.downloadRepository

    val state: StateFlow<DownloadsUiState> = repo.tasks
        .map { DownloadsUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadsUiState())

    fun pause(id: Long) = viewModelScope.launch { repo.pause(id) }
    fun resume(id: Long) = viewModelScope.launch { repo.resume(id) }
    fun cancel(id: Long) = viewModelScope.launch { repo.cancel(id) }
    fun pauseAll() = viewModelScope.launch { repo.pauseAll() }
    fun resumeAll() = viewModelScope.launch { repo.resumeAll() }
    fun cancelAll() = viewModelScope.launch { repo.cancelAll() }
    fun clearFinished() = viewModelScope.launch { repo.clearFinished() }
}

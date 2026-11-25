package com.example.xdownloader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xdownloader.data.local.UserPreferencesRepository
import com.example.xdownloader.data.remote.ytdlp.YtDlpManager
import com.example.xdownloader.domain.model.QualityPreference
import com.example.xdownloader.domain.model.ThemeMode
import com.example.xdownloader.domain.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val ytDlpManager: YtDlpManager
) : ViewModel() {

    val userSettings: StateFlow<UserSettings> = preferencesRepository.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    fun updateAutoDownloadOnShare(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateAutoDownloadOnShare(enabled)
        }
    }

    fun updateDefaultQuality(quality: QualityPreference) {
        viewModelScope.launch {
            preferencesRepository.updateDefaultQuality(quality)
        }
    }

    fun updateWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateWifiOnly(wifiOnly)
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.updateThemeMode(themeMode)
        }
    }

    fun updateFloatingBubbleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateFloatingBubbleEnabled(enabled)
        }
    }

    fun updateYtDlpEngine(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = ytDlpManager.updateYtDlp()
            onResult(result)
        }
    }
}

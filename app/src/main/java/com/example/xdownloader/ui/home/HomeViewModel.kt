package com.example.xdownloader.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.xdownloader.data.local.DownloadedVideoDao
import com.example.xdownloader.data.local.DownloadedVideoEntity
import com.example.xdownloader.data.local.UserPreferencesRepository
import com.example.xdownloader.data.repository.ExtractionRepository
import com.example.xdownloader.domain.model.DownloadStatus
import com.example.xdownloader.domain.model.ExtractionResult
import com.example.xdownloader.domain.model.QualityPreference
import com.example.xdownloader.domain.model.TweetMetadata
import com.example.xdownloader.domain.model.UserSettings
import com.example.xdownloader.domain.model.VideoVariant
import com.example.xdownloader.util.WorkManagerUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class HomeUiState {
    object Idle : HomeUiState()
    object Resolving : HomeUiState()
    data class MetadataResolved(
        val metadata: TweetMetadata,
        val selectedVariant: VideoVariant
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val extractionRepository: ExtractionRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val dao: DownloadedVideoDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    var inputUrl = MutableStateFlow("")

    val userSettings: StateFlow<UserSettings> = preferencesRepository.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val allDownloads: StateFlow<List<DownloadedVideoEntity>> = dao.getAllVideosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun resolveUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            _uiState.value = HomeUiState.Resolving
            when (val result = extractionRepository.extractVideo(trimmed)) {
                is ExtractionResult.Success -> {
                    val metadata = result.metadata
                    val mediaItem = metadata.mediaItems.firstOrNull()
                    if (mediaItem == null || mediaItem.variants.isEmpty()) {
                        _uiState.value = HomeUiState.Error("No downloadable MP4 video found in this tweet.")
                        return@launch
                    }
                    val settings = withContext(Dispatchers.IO) {
                        preferencesRepository.userSettingsFlow.first()
                    }
                    val bestVariant = selectVariantByPreference(mediaItem.variants, settings.defaultQuality)
                    _uiState.value = HomeUiState.MetadataResolved(metadata, bestVariant)
                }
                is ExtractionResult.Error -> {
                    _uiState.value = HomeUiState.Error(result.message)
                }
            }
        }
    }

    fun downloadVideo(context: Context, metadata: TweetMetadata, variant: VideoVariant) {
        viewModelScope.launch {
            val settings = withContext(Dispatchers.IO) {
                preferencesRepository.userSettingsFlow.first()
            }
            val platform = extractionRepository.detectPlatform(metadata.originalUrl) ?: com.example.xdownloader.domain.model.MediaPlatform.X_TWITTER
            WorkManagerUtils.enqueueVideoDownload(context, metadata, variant, settings.wifiOnly, platform)
            _uiState.value = HomeUiState.Idle
            inputUrl.value = ""
        }
    }

    fun toggleFloatingBubble(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateFloatingBubbleEnabled(enabled)
        }
    }

    fun cancelDownload(context: Context, videoId: String) {
        viewModelScope.launch {
            WorkManager.getInstance(context).cancelUniqueWork(videoId)
            withContext(Dispatchers.IO) {
                dao.updateDownloadStatus(videoId, DownloadStatus.FAILED)
            }
        }
    }

    private fun selectVariantByPreference(
        variants: List<VideoVariant>,
        preference: QualityPreference
    ): VideoVariant {
        return when (preference) {
            QualityPreference.BEST, QualityPreference.ALWAYS_ASK -> variants.first()
            QualityPreference.HD_720P -> {
                variants.find { it.resolution.contains("720p") }
                    ?: variants.find { it.resolution.contains("1080p") }
                    ?: variants.first()
            }
            QualityPreference.SD_480P -> {
                variants.find { it.resolution.contains("480p") }
                    ?: variants.find { it.resolution.contains("360p") }
                    ?: variants.last()
            }
        }
    }
}

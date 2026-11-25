package com.example.xdownloader.ui.share

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xdownloader.data.local.UserPreferencesRepository
import com.example.xdownloader.data.repository.ExtractionRepository
import com.example.xdownloader.domain.model.ExtractionResult
import com.example.xdownloader.domain.model.QualityPreference
import com.example.xdownloader.domain.model.TweetMetadata
import com.example.xdownloader.domain.model.VideoVariant
import com.example.xdownloader.util.WorkManagerUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class ShareUiState {
    object Idle : ShareUiState()
    object Resolving : ShareUiState()
    object NoLinkInClipboard : ShareUiState()
    data class MetadataLoaded(
        val metadata: TweetMetadata,
        val selectedVariant: VideoVariant,
        val autoDownloadStarted: Boolean
    ) : ShareUiState()
    data class Error(val message: String) : ShareUiState()
}

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val extractionRepository: ExtractionRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShareUiState>(ShareUiState.Idle)
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    fun setCheckingClipboard() {
        _uiState.value = ShareUiState.Resolving
    }

    fun showManualPasteInput() {
        _uiState.value = ShareUiState.NoLinkInClipboard
    }

    fun resolveText(context: Context, sharedText: String) {
        if (sharedText.isBlank()) {
            _uiState.value = ShareUiState.NoLinkInClipboard
            return
        }

        viewModelScope.launch {
            _uiState.value = ShareUiState.Resolving
            val userSettings = withContext(Dispatchers.IO) {
                preferencesRepository.userSettingsFlow.first()
            }

            when (val result = extractionRepository.extractVideo(sharedText)) {
                is ExtractionResult.Success -> {
                    val metadata = result.metadata
                    val mediaItem = metadata.mediaItems.firstOrNull()

                    if (mediaItem == null || mediaItem.variants.isEmpty()) {
                        _uiState.value = ShareUiState.Error("No downloadable MP4 video streams found in this tweet.")
                        return@launch
                    }

                    val variants = mediaItem.variants
                    val selectedVariant = selectVariantByPreference(variants, userSettings.defaultQuality)

                    if (userSettings.autoDownloadOnShare && userSettings.defaultQuality != QualityPreference.ALWAYS_ASK) {
                        WorkManagerUtils.enqueueVideoDownload(
                            context = context,
                            metadata = metadata,
                            variant = selectedVariant,
                            wifiOnly = userSettings.wifiOnly
                        )
                        _uiState.value = ShareUiState.MetadataLoaded(
                            metadata = metadata,
                            selectedVariant = selectedVariant,
                            autoDownloadStarted = true
                        )
                    } else {
                        _uiState.value = ShareUiState.MetadataLoaded(
                            metadata = metadata,
                            selectedVariant = selectedVariant,
                            autoDownloadStarted = false
                        )
                    }
                }
                is ExtractionResult.Error -> {
                    _uiState.value = ShareUiState.Error(result.message)
                }
            }
        }
    }

    fun downloadVariant(context: Context, metadata: TweetMetadata, variant: VideoVariant) {
        viewModelScope.launch {
            val settings = withContext(Dispatchers.IO) {
                preferencesRepository.userSettingsFlow.first()
            }
            WorkManagerUtils.enqueueVideoDownload(context, metadata, variant, settings.wifiOnly)
            _uiState.value = ShareUiState.MetadataLoaded(metadata, variant, autoDownloadStarted = true)
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

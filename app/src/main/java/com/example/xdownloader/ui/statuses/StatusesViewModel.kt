package com.example.xdownloader.ui.statuses

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xdownloader.data.local.UserPreferencesRepository
import com.example.xdownloader.data.repository.WhatsAppStatusRepository
import com.example.xdownloader.domain.model.UserSettings
import com.example.xdownloader.domain.model.WhatsAppStatusItem
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

enum class StatusFilter {
    ALL,
    VIDEOS,
    PHOTOS
}

@HiltViewModel
class StatusesViewModel @Inject constructor(
    private val whatsappRepository: WhatsAppStatusRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val userSettings: StateFlow<UserSettings> = preferencesRepository.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _statuses = MutableStateFlow<List<WhatsAppStatusItem>>(emptyList())
    val statuses: StateFlow<List<WhatsAppStatusItem>> = _statuses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val selectedFilter = MutableStateFlow(StatusFilter.ALL)
    val isBusinessTab = MutableStateFlow(false)

    fun loadStatuses() {
        viewModelScope.launch {
            _isLoading.value = true
            val settings = withContext(Dispatchers.IO) { preferencesRepository.userSettingsFlow.first() }
            val isBusiness = isBusinessTab.value
            val treeUri = if (isBusiness) settings.w4bTreeUri else settings.whatsappTreeUri

            val items = whatsappRepository.loadStatuses(treeUri, isBusiness)
            _statuses.value = items
            _isLoading.value = false
        }
    }

    fun saveWhatsAppPermission(context: Context, treeUri: Uri, isBusiness: Boolean) {
        viewModelScope.launch {
            try {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Permission may already be persisted
            }

            if (isBusiness) {
                preferencesRepository.updateW4bTreeUri(treeUri.toString())
            } else {
                preferencesRepository.updateWhatsAppTreeUri(treeUri.toString())
            }
            loadStatuses()
        }
    }

    fun saveStatus(item: WhatsAppStatusItem, onSaved: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = whatsappRepository.saveStatus(item)
            if (success) {
                _statuses.value = _statuses.value.map {
                    if (it.id == item.id) it.copy(isSaved = true) else it
                }
            }
            onSaved(success)
        }
    }
}

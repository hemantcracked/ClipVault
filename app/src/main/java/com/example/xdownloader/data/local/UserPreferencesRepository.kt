package com.example.xdownloader.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.xdownloader.domain.model.QualityPreference
import com.example.xdownloader.domain.model.ThemeMode
import com.example.xdownloader.domain.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val AUTO_DOWNLOAD_ON_SHARE = booleanPreferencesKey("auto_download_on_share")
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FLOATING_BUBBLE_ENABLED = booleanPreferencesKey("floating_bubble_enabled")
        val WHATSAPP_TREE_URI = stringPreferencesKey("whatsapp_tree_uri")
        val W4B_TREE_URI = stringPreferencesKey("w4b_tree_uri")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val autoDownload = preferences[PreferencesKeys.AUTO_DOWNLOAD_ON_SHARE] ?: true
        val qualityStr = preferences[PreferencesKeys.DEFAULT_QUALITY] ?: QualityPreference.BEST.name
        val wifiOnly = preferences[PreferencesKeys.WIFI_ONLY] ?: false
        val themeStr = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val bubbleEnabled = preferences[PreferencesKeys.FLOATING_BUBBLE_ENABLED] ?: false
        val whatsappUri = preferences[PreferencesKeys.WHATSAPP_TREE_URI] ?: ""
        val w4bUri = preferences[PreferencesKeys.W4B_TREE_URI] ?: ""

        val quality = try {
            QualityPreference.valueOf(qualityStr)
        } catch (e: Exception) {
            QualityPreference.BEST
        }

        UserSettings(
            autoDownloadOnShare = autoDownload,
            defaultQuality = quality,
            wifiOnly = wifiOnly,
            themeMode = themeStr.let {
                try { ThemeMode.valueOf(it) } catch (e: Exception) { ThemeMode.SYSTEM }
            },
            floatingBubbleEnabled = bubbleEnabled,
            whatsappTreeUri = whatsappUri,
            w4bTreeUri = w4bUri
        )
    }.flowOn(Dispatchers.IO)

    suspend fun updateAutoDownloadOnShare(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_DOWNLOAD_ON_SHARE] = enabled
        }
    }

    suspend fun updateDefaultQuality(quality: QualityPreference) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_QUALITY] = quality.name
        }
    }

    suspend fun updateWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIFI_ONLY] = wifiOnly
        }
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateFloatingBubbleEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FLOATING_BUBBLE_ENABLED] = enabled
        }
    }

    suspend fun updateWhatsAppTreeUri(uriString: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WHATSAPP_TREE_URI] = uriString
        }
    }

    suspend fun updateW4bTreeUri(uriString: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.W4B_TREE_URI] = uriString
        }
    }
}

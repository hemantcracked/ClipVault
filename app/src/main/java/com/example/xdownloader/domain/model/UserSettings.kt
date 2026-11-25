package com.example.xdownloader.domain.model

enum class QualityPreference {
    BEST,
    HD_720P,
    SD_480P,
    ALWAYS_ASK
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED_BLACK
}

data class UserSettings(
    val autoDownloadOnShare: Boolean = true,
    val defaultQuality: QualityPreference = QualityPreference.BEST,
    val wifiOnly: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val floatingBubbleEnabled: Boolean = false,
    val whatsappTreeUri: String = "",
    val w4bTreeUri: String = ""
)

package com.example.xdownloader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.xdownloader.domain.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = OnIndigoPrimaryLight,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = OnIndigoContainerLight,
    secondary = CyanSecondaryLight,
    onSecondary = OnCyanSecondaryLight,
    secondaryContainer = CyanContainerLight,
    onSecondaryContainer = OnCyanContainerLight,
    tertiary = EmeraldTertiaryLight,
    onTertiary = OnEmeraldTertiaryLight,
    tertiaryContainer = EmeraldContainerLight,
    onTertiaryContainer = OnEmeraldTertiaryContainerLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = OnIndigoPrimaryDark,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = OnIndigoContainerDark,
    secondary = CyanSecondaryDark,
    onSecondary = OnCyanSecondaryDark,
    secondaryContainer = CyanContainerDark,
    onSecondaryContainer = OnCyanContainerDark,
    tertiary = EmeraldTertiaryDark,
    onTertiary = OnEmeraldTertiaryDark,
    tertiaryContainer = EmeraldContainerDark,
    onTertiaryContainer = OnEmeraldContainerDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

private val AmoledColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = OnIndigoPrimaryDark,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = OnIndigoContainerDark,
    secondary = CyanSecondaryDark,
    onSecondary = OnCyanSecondaryDark,
    secondaryContainer = CyanContainerDark,
    onSecondaryContainer = OnCyanContainerDark,
    tertiary = EmeraldTertiaryDark,
    onTertiary = OnEmeraldTertiaryDark,
    tertiaryContainer = EmeraldContainerDark,
    onTertiaryContainer = OnEmeraldContainerDark,
    background = AmoledBackground,
    surface = AmoledSurface,
    surfaceVariant = AmoledSurfaceVariant,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = AmoledOutlineVariant
)

@Composable
fun ClipVaultTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    XDownloaderTheme(themeMode = themeMode, content = content)
}

@Composable
fun XDownloaderTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED_BLACK -> true
    }

    val colorScheme = when {
        themeMode == ThemeMode.AMOLED_BLACK -> AmoledColorScheme
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

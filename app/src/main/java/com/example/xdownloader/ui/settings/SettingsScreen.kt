package com.example.xdownloader.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.xdownloader.domain.model.QualityPreference
import com.example.xdownloader.domain.model.ThemeMode
import com.example.xdownloader.service.FloatingOverlayService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.userSettings.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "App Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Customize download behavior, quality, and app theme.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Instant Download Toggle
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Instant 1-Tap HD Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Automatically start download when a link is shared to ClipVault.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.autoDownloadOnShare,
                    onCheckedChange = { viewModel.updateAutoDownloadOnShare(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Floating Bubble Toggle over X
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BubbleChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Floating Download Bubble over X",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hovering download icon over X for 1-tap clipboard downloads.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.floatingBubbleEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (!Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                viewModel.updateFloatingBubbleEnabled(true)
                                val serviceIntent = Intent(context, FloatingOverlayService::class.java)
                                ContextCompat.startForegroundService(context, serviceIntent)
                            }
                        } else {
                            viewModel.updateFloatingBubbleEnabled(false)
                            val serviceIntent = Intent(context, FloatingOverlayService::class.java)
                            context.stopService(serviceIntent)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Wi-Fi Only Toggle
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Download over Wi-Fi Only",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pause downloads when using mobile data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.wifiOnly,
                    onCheckedChange = { viewModel.updateWifiOnly(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preferred Quality Dropdown
        var qualityExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = qualityExpanded,
            onExpandedChange = { qualityExpanded = it }
        ) {
            OutlinedTextField(
                value = when (settings.defaultQuality) {
                    QualityPreference.BEST -> "Best Available (Highest Bitrate)"
                    QualityPreference.HD_720P -> "720p HD"
                    QualityPreference.SD_480P -> "480p SD"
                    QualityPreference.ALWAYS_ASK -> "Always Ask Quality"
                },
                onValueChange = {},
                readOnly = true,
                label = { Text("Default Download Quality") },
                leadingIcon = { Icon(Icons.Default.HighQuality, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = qualityExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = qualityExpanded,
                onDismissRequest = { qualityExpanded = false }
            ) {
                QualityPreference.entries.forEach { pref ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                when (pref) {
                                    QualityPreference.BEST -> "Best Available (Highest Bitrate)"
                                    QualityPreference.HD_720P -> "720p HD"
                                    QualityPreference.SD_480P -> "480p SD"
                                    QualityPreference.ALWAYS_ASK -> "Always Ask Quality"
                                }
                            )
                        },
                        onClick = {
                            viewModel.updateDefaultQuality(pref)
                            qualityExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Theme Selector Dropdown
        var themeExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = themeExpanded,
            onExpandedChange = { themeExpanded = it }
        ) {
            OutlinedTextField(
                value = when (settings.themeMode) {
                    ThemeMode.SYSTEM -> "System Default"
                    ThemeMode.LIGHT -> "Light Theme"
                    ThemeMode.DARK -> "Dark Theme"
                    ThemeMode.AMOLED_BLACK -> "Pure AMOLED Black"
                },
                onValueChange = {},
                readOnly = true,
                label = { Text("App Theme") },
                leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = themeExpanded,
                onDismissRequest = { themeExpanded = false }
            ) {
                ThemeMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> "System Default"
                                    ThemeMode.LIGHT -> "Light Theme"
                                    ThemeMode.DARK -> "Dark Theme"
                                    ThemeMode.AMOLED_BLACK -> "Pure AMOLED Black"
                                }
                            )
                        },
                        onClick = {
                            viewModel.updateThemeMode(mode)
                            themeExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Update Extractor Engine Card
        var isUpdatingYtDlp by remember { mutableStateOf(false) }
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Update Extractor Engine (yt-dlp)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Update extractor binaries to fix Instagram or YouTube extraction changes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                androidx.compose.material3.Button(
                    onClick = {
                        isUpdatingYtDlp = true
                        viewModel.updateYtDlpEngine { result ->
                            isUpdatingYtDlp = false
                            android.widget.Toast.makeText(context, "yt-dlp: $result", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isUpdatingYtDlp
                ) {
                    if (isUpdatingYtDlp) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Update")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(16.dp))

        // About & Developer Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ClipVault v1.0.0",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "All-in-One Media Saver",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                // Developer Website Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://hemanthpulicharla.pythonanywhere.com")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try { context.startActivity(intent) } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Could not open browser", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Developer Website", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("hemanthpulicharla.pythonanywhere.com", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // WhatsApp Support Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/917893782351")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(waIntent)
                            } catch (e: Exception) {
                                try {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+917893782351")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(dialIntent)
                                } catch (ex: Exception) {
                                    android.widget.Toast.makeText(context, "Could not contact support", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("WhatsApp Support", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("+91 78937 82351", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Email Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val mailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:hemanthdevloper@gmail.com?subject=ClipVault%20App")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(mailIntent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Could not open email client", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Developer Email", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("hemanthdevloper@gmail.com", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Copyright Footer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://hemanthpulicharla.pythonanywhere.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try { context.startActivity(intent) } catch (e: Exception) {}
                }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "© 2026 Hemanth Pulicharla. All rights reserved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

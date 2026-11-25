package com.example.xdownloader.ui.home

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.xdownloader.data.local.DownloadedVideoEntity
import com.example.xdownloader.domain.model.DownloadStatus
import com.example.xdownloader.domain.model.TweetMetadata
import com.example.xdownloader.domain.model.VideoVariant
import com.example.xdownloader.service.FloatingOverlayService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val urlInput by viewModel.inputUrl.collectAsState()
    val downloads by viewModel.allDownloads.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()

    var clipboardUrl by remember { mutableStateOf<String?>(null) }
    var isTipVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            val lower = text.lowercase()
            if (lower.contains("x.com/") || lower.contains("twitter.com/") || lower.contains("t.co/") || lower.contains("instagram.com/") || lower.contains("instagr.am/") || lower.contains("youtube.com/") || lower.contains("youtu.be/")) {
                clipboardUrl = text
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header with Floating Bubble Pill Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ClipVault",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "All-in-One Media Saver",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Floating Bubble Header Pill
                Surface(
                    color = if (userSettings.floatingBubbleEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clickable {
                        if (!userSettings.floatingBubbleEnabled) {
                            if (!Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                viewModel.toggleFloatingBubble(true)
                                ContextCompat.startForegroundService(context, Intent(context, FloatingOverlayService::class.java))
                            }
                        } else {
                            viewModel.toggleFloatingBubble(false)
                            context.stopService(Intent(context, FloatingOverlayService::class.java))
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (userSettings.floatingBubbleEnabled) Color(0xFF4CAF50) else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.BubbleChart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (userSettings.floatingBubbleEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (userSettings.floatingBubbleEnabled) "Bubble Active" else "Bubble Off",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (userSettings.floatingBubbleEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Hero Download Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Download Media",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "YouTube • Instagram • X • WhatsApp",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { viewModel.inputUrl.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Paste X, Instagram, or YouTube link...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Link, contentDescription = null)
                        },
                        trailingIcon = {
                            if (urlInput.isNotEmpty()) {
                                IconButton(onClick = { viewModel.inputUrl.value = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            } else {
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = clipboard?.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        viewModel.inputUrl.value = clip.getItemAt(0).text?.toString() ?: ""
                                    }
                                }) {
                                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.resolveUrl(urlInput) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = urlInput.isNotBlank() && uiState !is HomeUiState.Resolving,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState is HomeUiState.Resolving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Fetching Video...")
                        } else {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download Video")
                        }
                    }
                }
            }
        }

        // Smart Clipboard Card Pill
        if (clipboardUrl != null && urlInput.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.inputUrl.value = clipboardUrl!!
                            viewModel.resolveUrl(clipboardUrl!!)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "X link detected in clipboard",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Tap to fetch and download instantly",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Resolved Preview Card / Error Banner
        item {
            when (val state = uiState) {
                is HomeUiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                is HomeUiState.MetadataResolved -> {
                    ResolvedPreviewCard(
                        metadata = state.metadata,
                        initialVariant = state.selectedVariant,
                        onDownloadClicked = { variant ->
                            viewModel.downloadVideo(context, state.metadata, variant)
                        }
                    )
                }
                else -> {}
            }
        }

        // Active Downloads Section
        val activeList = downloads.filter { it.downloadStatus == DownloadStatus.DOWNLOADING || it.downloadStatus == DownloadStatus.QUEUED }
        if (activeList.isNotEmpty()) {
            item {
                Text(
                    text = "Active Downloads",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(activeList, key = { it.id }) { item ->
                ActiveDownloadCard(
                    item = item,
                    onCancelClicked = { viewModel.cancelDownload(context, item.id) }
                )
            }
        }

        // Quick Tip Strip
        if (isTipVisible) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Pro tip: Inside any app, tap Share → Save with ClipVault for instant downloads.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { isTipVisible = false }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResolvedPreviewCard(
    metadata: TweetMetadata,
    initialVariant: VideoVariant,
    onDownloadClicked: (VideoVariant) -> Unit
) {
    var selectedVariant by remember { mutableStateOf(initialVariant) }
    val mediaItem = metadata.mediaItems.firstOrNull() ?: return
    val variants = mediaItem.variants

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = metadata.authorAvatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = metadata.authorName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${metadata.authorHandle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (metadata.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = metadata.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            AsyncImage(
                model = mediaItem.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Select Resolution:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                variants.forEach { variant ->
                    val sizeMb = if (variant.estimatedSizeBytes > 0) " (${variant.estimatedSizeBytes / (1024 * 1024)} MB)" else ""
                    FilterChip(
                        selected = variant.url == selectedVariant.url,
                        onClick = { selectedVariant = variant },
                        label = { Text("${variant.resolution}$sizeMb") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onDownloadClicked(selectedVariant) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Downloading ${selectedVariant.resolution}")
            }
        }
    }
}

@Composable
fun ActiveDownloadCard(
    item: DownloadedVideoEntity,
    onCancelClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "@${item.authorHandle}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Downloading ${item.resolution}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onCancelClicked) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

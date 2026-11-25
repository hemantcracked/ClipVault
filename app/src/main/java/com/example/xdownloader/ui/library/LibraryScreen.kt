package com.example.xdownloader.ui.library

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.xdownloader.data.local.DownloadedVideoEntity
import com.example.xdownloader.domain.model.MediaPlatform
import com.example.xdownloader.ui.player.PlayerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPlatform by viewModel.selectedPlatform.collectAsState()
    val videos by viewModel.videos.collectAsState()

    var playingVideo by remember { mutableStateOf<DownloadedVideoEntity?>(null) }
    var viewingImage by remember { mutableStateOf<DownloadedVideoEntity?>(null) }
    var videoToDelete by remember { mutableStateOf<DownloadedVideoEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Saved Library",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Watch, manage, and share your downloaded media offline.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by @handle or caption...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Platform Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedPlatform == "ALL",
                onClick = { viewModel.selectedPlatform.value = "ALL" },
                label = { Text("All") }
            )
            FilterChip(
                selected = selectedPlatform == MediaPlatform.X_TWITTER.name,
                onClick = { viewModel.selectedPlatform.value = MediaPlatform.X_TWITTER.name },
                label = { Text("X") }
            )
            FilterChip(
                selected = selectedPlatform == MediaPlatform.INSTAGRAM.name,
                onClick = { viewModel.selectedPlatform.value = MediaPlatform.INSTAGRAM.name },
                label = { Text("Instagram") }
            )
            FilterChip(
                selected = selectedPlatform == MediaPlatform.YOUTUBE.name,
                onClick = { viewModel.selectedPlatform.value = MediaPlatform.YOUTUBE.name },
                label = { Text("YouTube") }
            )
            FilterChip(
                selected = selectedPlatform == MediaPlatform.WHATSAPP.name,
                onClick = { viewModel.selectedPlatform.value = MediaPlatform.WHATSAPP.name },
                label = { Text("WhatsApp") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (videos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "No saved media yet." else "No matching media found.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Download videos or save WhatsApp statuses to view here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(videos, key = { it.id }) { item ->
                    GridMediaCard(
                        video = item,
                        onMediaClicked = {
                            if (item.mediaType == "IMAGE") {
                                viewingImage = item
                            } else {
                                playingVideo = item
                            }
                        },
                        onShareClicked = { viewModel.shareVideoFile(context, item) },
                        onOpenTweetClicked = { viewModel.openOriginalTweet(context, item) },
                        onDeleteClicked = { videoToDelete = item }
                    )
                }
            }
        }
    }

    playingVideo?.let { video ->
        PlayerDialog(
            videoUriString = video.mediaStoreUri,
            title = "@${video.authorHandle} - ${video.resolution}",
            onDismiss = { playingVideo = null }
        )
    }

    viewingImage?.let { image ->
        Dialog(
            onDismissRequest = { viewingImage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "@${image.authorHandle}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewingImage = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = image.mediaStoreUri.ifBlank { image.thumbnailUrl },
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(onClick = { viewModel.shareVideoFile(context, image) }) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share")
                        }
                    }
                }
            }
        }
    }

    videoToDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("Delete Media?") },
            text = { Text("This will delete @${video.authorHandle}'s media from both your library and phone storage.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteVideo(video)
                        videoToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GridMediaCard(
    video: DownloadedVideoEntity,
    onMediaClicked: () -> Unit,
    onShareClicked: () -> Unit,
    onOpenTweetClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .clickable { onMediaClicked() }
            ) {
                AsyncImage(
                    model = video.thumbnailUrl.ifBlank { video.mediaStoreUri },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (video.mediaType != "IMAGE") {
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // Top-Left Platform Badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = when (video.platform) {
                            MediaPlatform.YOUTUBE.name -> "YouTube"
                            MediaPlatform.INSTAGRAM.name -> "Instagram"
                            MediaPlatform.WHATSAPP.name -> "WhatsApp"
                            else -> "X"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Bottom-Right File Size Badge
                if (video.fileSizeBytes > 0) {
                    val sizeMb = video.fileSizeBytes / (1024 * 1024)
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "${sizeMb} MB",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Text Info & Overflow Menu
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "@${video.authorHandle}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share File") },
                                onClick = {
                                    menuExpanded = false
                                    onShareClicked()
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                            )
                            if (video.originalTweetUrl.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text("Open Post") },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenTweetClicked()
                                    },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClicked()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }

                if (video.caption.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = video.caption,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

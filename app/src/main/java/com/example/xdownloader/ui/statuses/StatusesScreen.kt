package com.example.xdownloader.ui.statuses

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.xdownloader.domain.model.MediaType
import com.example.xdownloader.domain.model.WhatsAppStatusItem
import com.example.xdownloader.ui.player.PlayerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusesScreen(
    viewModel: StatusesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.userSettings.collectAsState()
    val statuses by viewModel.statuses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val filter by viewModel.selectedFilter.collectAsState()
    val isBusinessTab by viewModel.isBusinessTab.collectAsState()

    var activeRequestingBusiness by remember { mutableStateOf(false) }
    var previewItem by remember { mutableStateOf<WhatsAppStatusItem?>(null) }

    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.saveWhatsAppPermission(context, uri, activeRequestingBusiness)
        }
    }

    fun requestFolderPermission(isBusiness: Boolean) {
        activeRequestingBusiness = isBusiness
        val initialUriStr = if (isBusiness) {
            "primary:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"
        } else {
            "primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
        }
        val initialUri = DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            initialUriStr
        )
        safLauncher.launch(initialUri)
    }

    LaunchedEffect(isBusinessTab) {
        viewModel.loadStatuses()
    }

    val currentTreeUri = if (isBusinessTab) settings.w4bTreeUri else settings.whatsappTreeUri
    val hasPermission = currentTreeUri.isNotBlank()

    val filteredList = statuses.filter { item ->
        when (filter) {
            StatusFilter.ALL -> true
            StatusFilter.VIDEOS -> item.mediaType == MediaType.VIDEO
            StatusFilter.PHOTOS -> item.mediaType == MediaType.IMAGE
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WhatsApp Status Saver",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "1-tap save videos & photos directly to Gallery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (hasPermission) {
                IconButton(onClick = { viewModel.loadStatuses() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // WhatsApp / WhatsApp Business Segmented Switcher
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !isBusinessTab,
                onClick = { viewModel.isBusinessTab.value = false },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("WhatsApp")
            }
            SegmentedButton(
                selected = isBusinessTab,
                onClick = { viewModel.isBusinessTab.value = true },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("WA Business")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!hasPermission) {
            // Permission Onboarding Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Grant Folder Access",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To view and save statuses on Android 11–16, grant access to the WhatsApp Statuses folder:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("1. Open WhatsApp & view any status", style = MaterialTheme.typography.bodySmall)
                    Text("2. Tap Grant Access below → 'Use this folder'", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(20.dp))
                    if (!isBusinessTab) {
                        Button(
                            onClick = { requestFolderPermission(false) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FolderSpecial, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant WhatsApp Access")
                        }
                    } else {
                        Button(
                            onClick = { requestFolderPermission(true) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FolderSpecial, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant WA Business Access")
                        }
                    }
                }
            }
        } else {
            // Filter Chips
            Row(modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = filter == StatusFilter.ALL,
                    onClick = { viewModel.selectedFilter.value = StatusFilter.ALL },
                    label = { Text("All") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    selected = filter == StatusFilter.VIDEOS,
                    onClick = { viewModel.selectedFilter.value = StatusFilter.VIDEOS },
                    label = { Text("Videos") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    selected = filter == StatusFilter.PHOTOS,
                    onClick = { viewModel.selectedFilter.value = StatusFilter.PHOTOS },
                    label = { Text("Photos") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No statuses found. Open WhatsApp and view some statuses first!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        StatusCard(
                            item = item,
                            onCardClicked = { previewItem = item },
                            onSaveClicked = {
                                viewModel.saveStatus(item) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    previewItem?.let { item ->
        if (item.mediaType == MediaType.VIDEO) {
            PlayerDialog(
                videoUriString = item.documentUri.toString(),
                title = if (item.isBusiness) "WA Business Status Video" else "WhatsApp Status Video",
                onDismiss = { previewItem = null }
            )
        } else {
            ImagePreviewDialog(
                item = item,
                onDismiss = { previewItem = null },
                onSave = {
                    viewModel.saveStatus(item) { success ->
                        if (success) Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                    }
                },
                onShare = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, item.documentUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Status"))
                }
            )
        }
    }
}

@Composable
fun StatusCard(
    item: WhatsAppStatusItem,
    onCardClicked: () -> Unit,
    onSaveClicked: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onCardClicked() }
        ) {
            AsyncImage(
                model = item.documentUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (item.mediaType == MediaType.VIDEO) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // Save Floating Button / Badge
            if (item.isSaved) {
                Surface(
                    color = Color(0xFF4CAF50),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Saved", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                IconButton(
                    onClick = onSaveClicked,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Save", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun ImagePreviewDialog(
    item: WhatsAppStatusItem,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
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
                        text = if (item.isBusiness) "WA Business Photo Status" else "WhatsApp Photo Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
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
                        model = item.documentUri,
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
                    OutlinedButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                    Button(onClick = onSave) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (item.isSaved) "Saved ✓" else "Save to Gallery")
                    }
                }
            }
        }
    }
}

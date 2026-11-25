package com.example.xdownloader.ui.share

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import coil.compose.AsyncImage
import com.example.xdownloader.domain.model.TweetMetadata
import com.example.xdownloader.domain.model.VideoVariant
import com.example.xdownloader.ui.theme.ClipVaultTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    companion object {
        private const val TAG = "XOverlay"
        const val ACTION_FROM_BUBBLE = "com.example.xdownloader.ACTION_FROM_BUBBLE"
    }

    private val viewModel: ShareViewModel by viewModels()
    private var pendingClipboardRead = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "SHEET_OPENED onCreate action=${intent?.action}")
        handleIncomingIntent(intent)

        setContent {
            ClipVaultTheme {
                ShareOverlayScreen(
                    viewModel = viewModel,
                    onDismiss = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d(TAG, "SHEET_OPENED onNewIntent action=${intent.action}")
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(incoming: Intent?) {
        if (incoming?.action == Intent.ACTION_SEND) {
            val sharedText = extractSharedText(incoming)
            Log.d(TAG, "ACTION_SEND extracted=${sharedText.take(120)}")
            pendingClipboardRead = false
            if (sharedText.isNotBlank()) viewModel.resolveText(this, sharedText)
            else viewModel.showManualPasteInput()
        } else {
            // Launched from Floating Bubble: wait for window focus + 350ms guaranteed fallback
            pendingClipboardRead = true
            viewModel.setCheckingClipboard()
            window.decorView.postDelayed({
                if (pendingClipboardRead && !isFinishing) {
                    readClipboardOnce("delayed_fallback")
                }
            }, 350L)
        }
    }

    private fun extractSharedText(intent: Intent): String {
        val candidates = mutableListOf<String>()
        intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.let { if (it.isNotBlank()) candidates.add(it) }
        intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString()?.let { if (it.isNotBlank()) candidates.add(it) }
        intent.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) {
                clip.getItemAt(i)?.coerceToText(this)?.toString()?.let { if (it.isNotBlank()) candidates.add(it) }
            }
        }
        intent.dataString?.let { if (it.isNotBlank()) candidates.add(it) }
        return candidates.distinct().joinToString("\n").trim()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d(TAG, "SHEET_FOCUS hasFocus=$hasFocus pending=$pendingClipboardRead")
        if (hasFocus && pendingClipboardRead) {
            readClipboardOnce("window_focus")
        }
    }

    private fun readClipboardOnce(source: String) {
        pendingClipboardRead = false
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        Log.d(TAG, "CLIPBOARD_READ($source) text=${text.take(120)}")
        if (text.contains("x.com", ignoreCase = true) ||
            text.contains("twitter.com", ignoreCase = true) ||
            text.contains("t.co/", ignoreCase = true) ||
            text.contains("instagram.com", ignoreCase = true) ||
            text.contains("instagr.am", ignoreCase = true) ||
            text.contains("youtube.com", ignoreCase = true) ||
            text.contains("youtu.be", ignoreCase = true)
        ) {
            viewModel.resolveText(this, text)
        } else {
            viewModel.showManualPasteInput()
        }
    }
}

@Composable
fun ShareOverlayScreen(
    viewModel: ShareViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is ShareUiState.MetadataLoaded && (uiState as ShareUiState.MetadataLoaded).autoDownloadStarted) {
            val state = uiState as ShareUiState.MetadataLoaded
            Toast.makeText(
                context,
                "Downloading ${state.metadata.authorHandle} HD Video (${state.selectedVariant.resolution})...",
                Toast.LENGTH_SHORT
            ).show()
            delay(1200)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onDismiss() }
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val state = uiState) {
                    is ShareUiState.Idle, is ShareUiState.Resolving -> {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Resolving Media Metadata...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    is ShareUiState.NoLinkInClipboard -> {
                        var inputUrl by remember { mutableStateOf("") }
                        Text(
                            text = "Paste Media Link",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            placeholder = { Text("Paste YouTube, Instagram, or X link...") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = clipboard?.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        inputUrl = clip.getItemAt(0).text?.toString() ?: ""
                                    }
                                }) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Copy a link, or tap Share → Save with ClipVault",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (inputUrl.isNotBlank()) {
                                    viewModel.resolveText(context, inputUrl)
                                }
                            },
                            enabled = inputUrl.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Paste & Download")
                        }
                    }

                    is ShareUiState.Error -> {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Download Failed",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = onDismiss) {
                            Text("Dismiss")
                        }
                    }

                    is ShareUiState.MetadataLoaded -> {
                        if (state.autoDownloadStarted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Downloading HD Video...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Downloading @${state.metadata.authorHandle} (${state.selectedVariant.resolution})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            QualityPickerContent(
                                metadata = state.metadata,
                                initialVariant = state.selectedVariant,
                                onDownloadClicked = { selected ->
                                    viewModel.downloadVariant(context, state.metadata, selected)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QualityPickerContent(
    metadata: TweetMetadata,
    initialVariant: VideoVariant,
    onDownloadClicked: (VideoVariant) -> Unit
) {
    var selectedVariant by remember { mutableStateOf(initialVariant) }
    val mediaItem = metadata.mediaItems.firstOrNull() ?: return
    val variants = mediaItem.variants

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = mediaItem.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "@${metadata.authorHandle}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = metadata.text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Select Download Quality:",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(modifier = Modifier.fillMaxWidth()) {
        variants.forEach { variant ->
            val labelText = when {
                variant.resolution.contains("1080p") -> "Best HD • 1080p"
                variant.resolution.contains("720p") -> "Medium • 720p"
                variant.resolution.contains("480p") -> "Data Saver • 480p"
                else -> variant.resolution
            }
            val sizeMb = if (variant.estimatedSizeBytes > 0) " (${variant.estimatedSizeBytes / (1024 * 1024)} MB)" else ""
            FilterChip(
                selected = variant.url == selectedVariant.url,
                onClick = { selectedVariant = variant },
                label = { Text("$labelText$sizeMb") },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))
    Button(
        onClick = { onDownloadClicked(selectedVariant) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(imageVector = Icons.Default.Download, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Start Download")
    }
}

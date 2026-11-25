package com.example.xdownloader.service

import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.xdownloader.MainActivity
import com.example.xdownloader.data.local.UserPreferencesRepository
import com.example.xdownloader.ui.share.ShareReceiverActivity
import com.example.xdownloader.ui.theme.ClipVaultTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class FloatingBubbleView(context: Context) : ImageView(context) {
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

@AndroidEntryPoint
class FloatingOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    companion object {
        private const val TAG = "XOverlay"
        const val NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "floating_bubble_channel"
    }

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private var bubbleView: FloatingBubbleView? = null

    private lateinit var trashParams: WindowManager.LayoutParams
    private var trashView: ComposeView? = null
    private var isTrashHovered = false

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var screenWidth = 1080

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val displaySize = Point()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getSize(displaySize)
        screenWidth = displaySize.x

        createNotificationChannel()
        setupTrashTarget()
        setupBubbleWindow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "SERVICE_START")
        startForegroundServiceInternal()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "SERVICE_DESTROY")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        hideTrashTarget()
        bubbleView?.let { try { windowManager.removeView(it) } catch (e: Exception) {} }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the floating download bubble active over X"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceInternal() {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ClipVault Floating Bubble")
            .setContentText("Tap floating icon anywhere to quick-download media")
            .setSmallIcon(com.example.ClipVault.R.drawable.ic_stat_clipvault)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            foregroundType
        )
    }

    private fun setupTrashTarget() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        }

        trashParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 120
        }

        trashView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayService)
            setViewTreeViewModelStoreOwner(this@FloatingOverlayService)

            setContent {
                ClipVaultTheme {
                    TrashTargetContent(isHovered = isTrashHovered)
                }
            }
        }
    }

    private fun setupBubbleWindow() {
        val bubbleSizePx = (56 * resources.displayMetrics.density).toInt()
        val initialX = screenWidth - bubbleSizePx - (16 * resources.displayMetrics.density).toInt()
        val initialY = (200 * resources.displayMetrics.density).toInt()

        bubbleParams = WindowManager.LayoutParams(
            bubbleSizePx,
            bubbleSizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }

        val backgroundDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xFF0061A4.toInt()) // Primary Blue
        }

        bubbleView = FloatingBubbleView(this).apply {
            isClickable = true
            isFocusable = true
            background = backgroundDrawable
            setImageResource(android.R.drawable.stat_sys_download)
            setColorFilter(Color.WHITE)
            val paddingPx = (14 * resources.displayMetrics.density).toInt()
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            elevation = 10 * resources.displayMetrics.density
        }

        setupDragAndClickTouchListener(bubbleView!!)

        try {
            windowManager.addView(bubbleView, bubbleParams)
            Log.d(TAG, "BUBBLE_ADDED")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding bubble window: ${e.message}", e)
        }
    }

    private fun setupDragAndClickTouchListener(view: FloatingBubbleView) {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d(TAG, "TOUCH_DOWN x=${event.rawX} y=${event.rawY}")
                    initialX = bubbleParams.x
                    initialY = bubbleParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    if (!isDragging && kotlin.math.hypot(dx.toDouble(), dy.toDouble()) > touchSlop * 1.5f) {
                        isDragging = true
                        showTrashTarget()
                    }

                    if (isDragging) {
                        bubbleParams.x = initialX + dx.toInt()
                        bubbleParams.y = initialY + dy.toInt()
                        try { windowManager.updateViewLayout(view, bubbleParams) } catch (e: Exception) {}
                        checkTrashHover(event.rawX, event.rawY)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    hideTrashTarget()
                    v.performClick()

                    if (isDragging) {
                        if (isTrashHovered) {
                            Log.d(TAG, "DRAG_DISMISSED")
                            serviceScope.launch {
                                preferencesRepository.updateFloatingBubbleEnabled(false)
                            }
                            stopSelf()
                        } else {
                            snapToNearestEdge()
                        }
                    } else {
                        Log.d(TAG, "TAP_DETECTED")
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onBubbleTapped()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    hideTrashTarget()
                    if (isDragging) {
                        snapToNearestEdge()
                    }
                    isDragging = false
                    true
                }
                else -> false
            }
        }
    }

    private fun showTrashTarget() {
        trashView?.let {
            if (it.windowToken == null) {
                try { windowManager.addView(it, trashParams) } catch (e: Exception) {}
            }
        }
    }

    private fun hideTrashTarget() {
        trashView?.let {
            if (it.windowToken != null) {
                try { windowManager.removeView(it) } catch (e: Exception) {}
            }
        }
        isTrashHovered = false
    }

    private fun checkTrashHover(rawX: Float, rawY: Float) {
        val displaySize = Point()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getSize(displaySize)
        val screenHeight = displaySize.y

        val trashCenterX = displaySize.x / 2
        val trashCenterY = screenHeight - 200

        val distance = kotlin.math.hypot((rawX - trashCenterX).toDouble(), (rawY - trashCenterY).toDouble())
        val hovered = distance < 200

        if (hovered != isTrashHovered) {
            isTrashHovered = hovered
            trashView?.setContent {
                ClipVaultTheme {
                    TrashTargetContent(isHovered = isTrashHovered)
                }
            }
        }
    }

    private fun snapToNearestEdge() {
        val targetX = if (bubbleParams.x + (bubbleView?.width ?: 120) / 2 < screenWidth / 2) {
            20
        } else {
            screenWidth - (bubbleView?.width ?: 140) - 20
        }

        val animator = ValueAnimator.ofInt(bubbleParams.x, targetX).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                bubbleParams.x = animation.animatedValue as Int
                bubbleView?.let { try { windowManager.updateViewLayout(it, bubbleParams) } catch (e: Exception) {} }
            }
        }
        animator.start()
    }

    private fun onBubbleTapped() {
        try {
            Log.d(TAG, "LAUNCHING_SHEET")
            val intent = Intent(this, ShareReceiverActivity::class.java).apply {
                action = ShareReceiverActivity.ACTION_FROM_BUBBLE
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "LAUNCH_ERROR: ${e.message}", e)
        }
    }
}

@Composable
fun TrashTargetContent(isHovered: Boolean) {
    Surface(
        color = if (isHovered) androidx.compose.ui.graphics.Color.Red else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.75f),
        shape = CircleShape,
        shadowElevation = 8.dp,
        modifier = Modifier
            .size(if (isHovered) 72.dp else 60.dp)
            .clip(CircleShape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss Floating Bubble",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(if (isHovered) 36.dp else 28.dp)
            )
        }
    }
}

package com.agradra.betterboyfriend.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.agradra.betterboyfriend.data.local.AppDatabase
import com.agradra.betterboyfriend.data.model.RuleItem
import com.agradra.betterboyfriend.data.model.RuleType
import com.agradra.betterboyfriend.ui.theme.BetterBoyFriendTheme
import com.agradra.betterboyfriend.util.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private var currentRules by mutableStateOf<List<RuleItem>>(emptyList())
    private var triggerTitle by mutableStateOf("트리거 알림")
    private var triggerCategory by mutableStateOf("일반")
    private var triggerEmoji by mutableStateOf("⚡")
    private var targetInfo by mutableStateOf("")

    private val handler = Handler(Looper.getMainLooper())
    private var autoDismissRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!PermissionHelper.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_TRIGGER_TITLE) ?: "주의 & 지침 알림"
        val category = intent?.getStringExtra(EXTRA_CATEGORY) ?: "일반"
        val emoji = intent?.getStringExtra(EXTRA_TRIGGER_EMOJI) ?: "⚡"
        val target = intent?.getStringExtra(EXTRA_TARGET_INFO) ?: ""

        triggerTitle = title
        triggerCategory = category
        triggerEmoji = emoji
        targetInfo = target

        // Load all rules matching the trigger's category
        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val allRules = withContext(Dispatchers.IO) { db.ruleItemDao().getAllRules() }

            val matched = if (category.isBlank() || category == "전체" || category == "일반") {
                allRules
            } else {
                val categoryMatched = allRules.filter { it.category.equals(category, ignoreCase = true) }
                if (categoryMatched.isNotEmpty()) categoryMatched else allRules
            }

            currentRules = matched
            showOrUpdateOverlay()
        }

        return START_NOT_STICKY
    }

    private fun showOrUpdateOverlay() {
        if (overlayView == null) {
            val owner = OverlayLifecycleOwner().also {
                it.onCreate()
                lifecycleOwner = it
            }

            // Cover full screen for 90% card & system blur behind
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_BLUR_BEHIND or
                        WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                dimAmount = 0.35f
                blurBehindRadius = 50
            }

            val composeView = ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setViewTreeViewModelStoreOwner(owner)

                setContent {
                    BetterBoyFriendTheme {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { dismissOverlay() },
                            contentAlignment = Alignment.Center
                        ) {
                            FloatingCardContent(
                                title = triggerTitle,
                                category = triggerCategory,
                                emoji = triggerEmoji,
                                target = targetInfo,
                                rules = currentRules,
                                onClose = { dismissOverlay() }
                            )
                        }
                    }
                }
            }

            try {
                windowManager?.addView(composeView, params)
                overlayView = composeView
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Auto-dismiss after 35 seconds
        autoDismissRunnable?.let { handler.removeCallbacks(it) }
        val runnable = Runnable { dismissOverlay() }
        autoDismissRunnable = runnable
        handler.postDelayed(runnable, 35_000)
    }

    private fun dismissOverlay() {
        autoDismissRunnable?.let { handler.removeCallbacks(it) }
        try {
            overlayView?.let {
                windowManager?.removeView(it)
                overlayView = null
            }
            lifecycleOwner?.onDestroy()
            lifecycleOwner = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissOverlay()
        serviceScope.cancel()
    }

    companion object {
        const val EXTRA_TRIGGER_TITLE = "extra_trigger_title"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_TRIGGER_EMOJI = "extra_trigger_emoji"
        const val EXTRA_TARGET_INFO = "extra_target_info"

        fun show(
            context: Context,
            title: String,
            category: String,
            emoji: String = "⚡",
            targetInfo: String = ""
        ) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                putExtra(EXTRA_TRIGGER_TITLE, title)
                putExtra(EXTRA_CATEGORY, category)
                putExtra(EXTRA_TRIGGER_EMOJI, emoji)
                putExtra(EXTRA_TARGET_INFO, targetInfo)
            }
            context.startService(intent)
        }
    }
}

@Composable
fun FloatingCardContent(
    title: String,
    category: String,
    emoji: String,
    target: String,
    rules: List<RuleItem>,
    onClose: () -> Unit
) {
    // 기본값은 항상 DONT
    var selectedType by remember { mutableStateOf(RuleType.DONT) }

    // 선택된 유형의 규칙들을 중요도 높은 순(3 -> 2 -> 1)으로 정렬
    val displayRules = remember(rules, selectedType) {
        rules.filter { it.type == selectedType }
            .sortedByDescending { it.priority }
    }

    // 가로/세로 화면의 90% 차지하는 반투명 글래스모피즘 카드
    Card(
        modifier = Modifier
            .fillMaxWidth(0.90f)
            .fillMaxHeight(0.90f)
            .clickable(enabled = false) {}, // 카드 내부 클릭 시 닫힘 방지
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
        border = BorderStroke(1.5.dp, selectedType.getComposePrimaryColor().copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            // Header (트리거 이름 및 카테고리)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = emoji, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#$category",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = selectedType.getComposePrimaryColor()
                        )
                        if (target.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "($target)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. [상단 / 가장 큰 영역] 리스트 (중요도 색상 점 + 내용)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (displayRules.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "'${selectedType.displayName}' 항목이 없습니다.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = displayRules, key = { it.id }) { rule ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 중요도 점 색깔 (3: 빨강, 2: 주황, 1: 파랑)
                                val dotColor = when (rule.priority) {
                                    3 -> Color(0xFFD32F2F) // Lv.3 빨강
                                    2 -> Color(0xFFF57C00) // Lv.2 주황
                                    else -> Color(0xFF1976D2) // Lv.1 파랑
                                }

                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(dotColor, CircleShape)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = rule.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 21.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. [중간] 버튼 3개 (DO, DONT, ASK)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    RuleType.DO to "Good",
                    RuleType.DONT to "Bad",
                    RuleType.ASK to "Ask"
                ).forEach { (type, label) ->
                    val isSelected = selectedType == type
                    val color = type.getComposePrimaryColor()
                    val containerColor = type.getComposeContainerColor()

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) containerColor.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isSelected) BorderStroke(1.5.dp, color) else null,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedType = type }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = type.iconEmoji, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = type.name, // DO, DONT, ASK
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. [하단] 확인 버튼 (닫기)
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "확인",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

class OverlayLifecycleOwner : SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore = store

    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

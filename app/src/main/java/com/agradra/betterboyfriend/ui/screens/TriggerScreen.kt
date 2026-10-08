package com.agradra.betterboyfriend.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.agradra.betterboyfriend.data.model.TriggerItem
import com.agradra.betterboyfriend.service.FloatingOverlayService
import com.agradra.betterboyfriend.ui.components.TriggerCard
import com.agradra.betterboyfriend.ui.components.TriggerDialog
import com.agradra.betterboyfriend.ui.viewmodel.TriggerViewModel
import com.agradra.betterboyfriend.util.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerScreen(
    viewModel: TriggerViewModel,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val triggers by viewModel.triggers.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var hasOverlayPermission by remember { mutableStateOf(PermissionHelper.canDrawOverlays(context)) }
    var hasNotifPermission by remember { mutableStateOf(PermissionHelper.isNotificationListenerEnabled(context)) }
    var hasAccessibilityPermission by remember { mutableStateOf(PermissionHelper.isAccessibilityEnabled(context)) }

    // Re-check permissions when coming back to foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = PermissionHelper.canDrawOverlays(context)
                hasNotifPermission = PermissionHelper.isNotificationListenerEnabled(context)
                hasAccessibilityPermission = PermissionHelper.isAccessibilityEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<TriggerItem?>(null) }
    var itemToDelete by remember { mutableStateOf<TriggerItem?>(null) }
    var itemForActionMenu by remember { mutableStateOf<TriggerItem?>(null) }
    var showPermissionAlert by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "트리거 설정",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "⚡", fontSize = 18.sp)
                    }
                },
                actions = {
                    // Test Floating Popup Button
                    IconButton(onClick = {
                        if (!hasOverlayPermission) {
                            showPermissionAlert = true
                        } else {
                            FloatingOverlayService.show(
                                context = context,
                                title = "미리보기 팝업",
                                category = if (availableCategories.isNotEmpty()) availableCategories.first() else "일반",
                                emoji = "⚡",
                                targetInfo = "테스트 실행"
                            )
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "팝업 테스트",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "트리거 추가")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Permission Banner Card
            item {
                PermissionStatusCard(
                    hasOverlay = hasOverlayPermission,
                    hasNotif = hasNotifPermission,
                    hasAccess = hasAccessibilityPermission,
                    onOpenOverlay = { PermissionHelper.openOverlaySettings(context) },
                    onOpenNotif = { PermissionHelper.openNotificationListenerSettings(context) },
                    onOpenAccess = { PermissionHelper.openAccessibilitySettings(context) }
                )
            }

            if (triggers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, bottom = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "⚡", fontSize = 44.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "등록된 트리거가 없습니다.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "우측 하단의 + 버튼을 눌러\n전화, 카톡/메시지, 특정 앱 실행 조건과\n연결할 카테고리를 등록해보세요!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(items = triggers, key = { it.id }) { trigger ->
                    TriggerCard(
                        trigger = trigger,
                        onToggle = { isEnabled ->
                            viewModel.toggleTrigger(trigger, isEnabled)
                        },
                        onClick = { itemForActionMenu = trigger },
                        onLongClick = { itemForActionMenu = trigger }
                    )
                }
            }
        }
    }

    // Permission Alert Dialog
    if (showPermissionAlert) {
        AlertDialog(
            onDismissRequest = { showPermissionAlert = false },
            title = { Text("오버레이 권한 필요") },
            text = {
                Text("팝업을 띄우려면 '다른 앱 위에 표시' 권한이 필요합니다. 설정 화면으로 이동하시겠습니까?")
            },
            confirmButton = {
                Button(onClick = {
                    showPermissionAlert = false
                    PermissionHelper.openOverlaySettings(context)
                }) {
                    Text("설정 열기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionAlert = false }) {
                    Text("취소")
                }
            }
        )
    }

    // Add Trigger Dialog
    if (showAddDialog) {
        TriggerDialog(
            initialItem = null,
            availableCategories = availableCategories,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, conditions, category, targetPerson, targetApp ->
                viewModel.addTrigger(title, conditions, category, targetPerson, targetApp)
                showAddDialog = false
            }
        )
    }

    // Edit Trigger Dialog
    itemToEdit?.let { item ->
        TriggerDialog(
            initialItem = item,
            availableCategories = availableCategories,
            onDismiss = { itemToEdit = null },
            onConfirm = { title, conditions, category, targetPerson, targetApp ->
                viewModel.updateTrigger(
                    item.copy(
                        title = title,
                        conditions = conditions,
                        category = category,
                        targetPerson = targetPerson,
                        targetApp = targetApp
                    )
                )
                itemToEdit = null
            }
        )
    }

    // Action Menu Dialog
    itemForActionMenu?.let { item ->
        AlertDialog(
            onDismissRequest = { itemForActionMenu = null },
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⚡", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "연결 카테고리: #${item.category}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "조건: " + item.conditions.joinToString(", ") { "${it.iconEmoji} ${it.shortName}" },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.targetPerson.isNotBlank()) {
                        Text(
                            text = "대상: ${item.targetPerson}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.targetApp.isNotBlank()) {
                        Text(
                            text = "대상 앱: ${item.targetApp}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Test This Trigger Button
                    Button(
                        onClick = {
                            if (!hasOverlayPermission) {
                                showPermissionAlert = true
                            } else {
                                val target = item
                                itemForActionMenu = null
                                FloatingOverlayService.show(
                                    context = context,
                                    title = target.title,
                                    category = target.category,
                                    emoji = target.conditions.firstOrNull()?.iconEmoji ?: "⚡",
                                    targetInfo = if (target.targetPerson.isNotBlank()) target.targetPerson else target.targetApp
                                )
                            }
                        }
                    ) {
                        Text("팝업 테스트")
                    }
                    OutlinedButton(
                        onClick = {
                            val target = item
                            itemForActionMenu = null
                            itemToEdit = target
                        }
                    ) {
                        Text("수정")
                    }
                    OutlinedButton(
                        onClick = {
                            val target = item
                            itemForActionMenu = null
                            itemToDelete = target
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("삭제")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { itemForActionMenu = null }) {
                    Text("닫기")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("트리거 삭제") },
            text = { Text("'${item.title}' 트리거를 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTrigger(item)
                        itemToDelete = null
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun PermissionStatusCard(
    hasOverlay: Boolean,
    hasNotif: Boolean,
    hasAccess: Boolean,
    onOpenOverlay: () -> Unit,
    onOpenNotif: () -> Unit,
    onOpenAccess: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(!hasOverlay || !hasNotif || !hasAccess) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasOverlay && hasNotif && hasAccess) Icons.Default.CheckCircle else Icons.Default.Settings,
                        contentDescription = null,
                        tint = if (hasOverlay && hasNotif && hasAccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "시스템 권한 상태",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                TextButton(onClick = { isExpanded = !isExpanded }) {
                    Text(text = if (isExpanded) "접기" else "자세히", fontSize = 11.sp)
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PermissionRowItem(
                        title = "📌 다른 앱 위에 표시 (팝업 필수)",
                        isGranted = hasOverlay,
                        onClick = onOpenOverlay
                    )
                    PermissionRowItem(
                        title = "💬 알림 접근 (카톡/메시지 감지)",
                        isGranted = hasNotif,
                        onClick = onOpenNotif
                    )
                    PermissionRowItem(
                        title = "📱 접근성 (앱 실행 감지)",
                        isGranted = hasAccess,
                        onClick = onOpenAccess
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRowItem(
    title: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (isGranted) {
                Text(
                    text = "허용됨 ✅",
                    fontSize = 11.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            } else {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(text = "설정", fontSize = 11.sp)
                }
            }
        }
    }
}

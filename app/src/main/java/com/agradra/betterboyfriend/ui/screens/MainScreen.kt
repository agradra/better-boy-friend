package com.agradra.betterboyfriend.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agradra.betterboyfriend.data.model.RuleItem
import com.agradra.betterboyfriend.data.model.RuleType
import com.agradra.betterboyfriend.ui.components.RuleCard
import com.agradra.betterboyfriend.ui.components.RuleDialog
import com.agradra.betterboyfriend.ui.viewmodel.RuleViewModel
import com.agradra.betterboyfriend.ui.viewmodel.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: RuleViewModel,
    onExportCsvClick: () -> Unit,
    onImportCsvClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilterType by viewModel.selectedFilterType.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<RuleItem?>(null) }
    var itemToDelete by remember { mutableStateOf<RuleItem?>(null) }
    var itemForActionMenu by remember { mutableStateOf<RuleItem?>(null) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "BetterBoyFriend",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "💖", fontSize = 18.sp)
                    }
                },
                actions = {
                    // Search toggle button
                    IconButton(onClick = {
                        showSearchBar = !showSearchBar
                        if (!showSearchBar) viewModel.setSearchQuery("")
                    }) {
                        Icon(
                            imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "검색"
                        )
                    }

                    // Sort menu button
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "정렬")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.displayName,
                                            fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // CSV & Overflow menu button
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "더보기")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("CSV로 내보내기") },
                                leadingIcon = {
                                    Icon(Icons.Default.FileUpload, contentDescription = null)
                                },
                                onClick = {
                                    showMenu = false
                                    onExportCsvClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("CSV 가져오기 (업데이트)") },
                                leadingIcon = {
                                    Icon(Icons.Default.FileDownload, contentDescription = null)
                                },
                                onClick = {
                                    showMenu = false
                                    onImportCsvClick()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "추가")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar (toggled)
            AnimatedVisibility(
                visible = showSearchBar,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("내용, 이유, 카테고리 검색...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "지우기")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Summary Tabs (All, DO, DONT, ASK)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All
                TypeSummaryCard(
                    title = "전체",
                    count = uiState.totalCount,
                    emoji = "📋",
                    isSelected = selectedFilterType == null,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setFilterType(null) }
                )

                // DO
                TypeSummaryCard(
                    title = "좋은 것",
                    count = uiState.doCount,
                    emoji = RuleType.DO.iconEmoji,
                    isSelected = selectedFilterType == RuleType.DO,
                    color = RuleType.DO.getComposePrimaryColor(),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setFilterType(if (selectedFilterType == RuleType.DO) null else RuleType.DO) }
                )

                // DONT
                TypeSummaryCard(
                    title = "안될 것",
                    count = uiState.dontCount,
                    emoji = RuleType.DONT.iconEmoji,
                    isSelected = selectedFilterType == RuleType.DONT,
                    color = RuleType.DONT.getComposePrimaryColor(),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setFilterType(if (selectedFilterType == RuleType.DONT) null else RuleType.DONT) }
                )

                // ASK
                TypeSummaryCard(
                    title = "물어볼 것",
                    count = uiState.askCount,
                    emoji = RuleType.ASK.iconEmoji,
                    isSelected = selectedFilterType == RuleType.ASK,
                    color = RuleType.ASK.getComposePrimaryColor(),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setFilterType(if (selectedFilterType == RuleType.ASK) null else RuleType.ASK) }
                )
            }

            // Category Chips Row (horizontal scroll)
            if (uiState.categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.setCategory(null) },
                        label = { Text("모든 카테고리", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                    uiState.categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                viewModel.setCategory(if (selectedCategory == cat) null else cat)
                            },
                            label = { Text("# $cat", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors()
                        )
                    }
                }
            }

            // Rules List or Empty State
            if (uiState.rules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "💌", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "검색 결과가 없습니다." else "등록된 지침이 없습니다.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "우측 하단의 + 버튼을 눌러\n하면 좋은 것, 안 되는 것, 물어볼 것을 등록해보세요!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = uiState.rules, key = { it.id }) { rule ->
                        RuleCard(
                            rule = rule,
                            onClick = { itemForActionMenu = rule },
                            onLongClick = { itemForActionMenu = rule }
                        )
                    }
                }
            }
        }
    }

    // Add Dialog
    if (showAddDialog) {
        RuleDialog(
            initialItem = null,
            todayDateString = viewModel.getTodayDateString(),
            availableCategories = uiState.categories,
            onDismiss = { showAddDialog = false },
            onConfirm = { type, title, priority, category, date, reason ->
                viewModel.addRule(type, title, priority, category, date, reason)
                showAddDialog = false
            }
        )
    }

    // Edit Dialog
    itemToEdit?.let { item ->
        RuleDialog(
            initialItem = item,
            todayDateString = viewModel.getTodayDateString(),
            availableCategories = uiState.categories,
            onDismiss = { itemToEdit = null },
            onConfirm = { type, title, priority, category, date, reason ->
                viewModel.updateRule(
                    item.copy(
                        type = type,
                        title = title,
                        priority = priority,
                        category = category,
                        date = date,
                        reason = reason
                    )
                )
                itemToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("삭제 확인") },
            text = { Text("'${item.title}' 항목을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRule(item)
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

    // Long Press / Tap Action Menu Dialog
    itemForActionMenu?.let { item ->
        AlertDialog(
            onDismissRequest = { itemForActionMenu = null },
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = item.type.iconEmoji, fontSize = 22.sp)
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
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "#${item.category}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = item.type.getComposePrimaryColor()
                        )
                        Text(
                            text = "• Lv.${item.priority}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.date.isNotBlank()) {
                            Text(
                                text = "• ${item.date}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (item.reason.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 이유: ${item.reason}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
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
}

@Composable
fun TypeSummaryCard(
    title: String,
    count: Int,
    emoji: String,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = if (isSelected) BorderStroke(1.5.dp, color) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "$count",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

package com.agradra.betterboyfriend.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.agradra.betterboyfriend.data.model.TriggerConditionType
import com.agradra.betterboyfriend.data.model.TriggerItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TriggerDialog(
    initialItem: TriggerItem? = null,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        conditions: List<TriggerConditionType>,
        category: String,
        targetPerson: String,
        targetApp: String
    ) -> Unit
) {
    val isEditMode = initialItem != null

    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    val selectedConditions = remember {
        mutableStateListOf<TriggerConditionType>().apply {
            if (initialItem != null) addAll(initialItem.conditions)
            else add(TriggerConditionType.CALL_RECEIVED)
        }
    }
    var category by remember { mutableStateOf(initialItem?.category ?: "일반") }
    var targetPerson by remember { mutableStateOf(initialItem?.targetPerson ?: "") }
    var targetApp by remember { mutableStateOf(initialItem?.targetApp ?: "") }

    var isConditionError by remember { mutableStateOf(false) }

    val presetCategories = remember(availableCategories) {
        val base = listOf("음식", "데이트", "연락", "선물", "대화", "행동", "패션")
        (availableCategories + base).distinct().filter { it.isNotBlank() }
    }

    val quickAppSuggestions = listOf("유튜브", "카카오톡", "인스타그램", "배달의민족", "토스", "당근마켓", "넷플릭스")

    val needsPerson = selectedConditions.contains(TriggerConditionType.CALL_RECEIVED) ||
            selectedConditions.contains(TriggerConditionType.MESSAGE_RECEIVED)
    val needsApp = selectedConditions.contains(TriggerConditionType.APP_OPENED)

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .heightIn(max = 700.dp),
        title = {
            Text(
                text = if (isEditMode) "트리거 수정" else "새 트리거 추가",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 조건 선택
                Text(
                    text = "발동 조건 (여러 개 선택 가능)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                TriggerConditionType.entries.forEach { condition ->
                    val isChecked = selectedConditions.contains(condition)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = if (isChecked) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                if (isChecked) {
                                    selectedConditions.remove(condition)
                                } else {
                                    selectedConditions.add(condition)
                                    isConditionError = false
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!selectedConditions.contains(condition)) selectedConditions.add(condition)
                                        isConditionError = false
                                    } else {
                                        selectedConditions.remove(condition)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = condition.iconEmoji, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = condition.displayName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = condition.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (isConditionError) {
                    Text(
                        text = "최소 하나 이상의 조건을 선택해주세요.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 대상자 입력
                if (needsPerson) {
                    OutlinedTextField(
                        value = targetPerson,
                        onValueChange = { targetPerson = it },
                        label = { Text("대상자 (이름 또는 전화번호)") },
                        placeholder = { Text("예: 여친, 010-1234-5678") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 대상 앱 입력
                if (needsApp) {
                    OutlinedTextField(
                        value = targetApp,
                        onValueChange = { targetApp = it },
                        label = { Text("감지할 앱 이름") },
                        placeholder = { Text("예: 유튜브, 카카오톡, 인스타") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        text = "💡 '유튜브', 'YouTube' 등 한글/영문/대소문자 구분 없이 모두 인식됩니다.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickAppSuggestions.forEach { appName ->
                            FilterChip(
                                selected = targetApp == appName,
                                onClick = { targetApp = appName },
                                label = { Text(appName, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 4. 연결할 카테고리
                Text(
                    text = "표시할 지침 카테고리",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("카테고리") },
                    placeholder = { Text("직접 입력 또는 아래 칩 클릭") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetCategories.take(8).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 트리거 이름 (선택)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("트리거 이름 (비워두면 자동 생성)") },
                    placeholder = { Text("예: 여친 연락 올 때, 배민 켤 때 등") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedConditions.isEmpty()) {
                        isConditionError = true
                        return@Button
                    }
                    onConfirm(
                        title,
                        selectedConditions.toList(),
                        category.ifBlank { "일반" },
                        targetPerson,
                        targetApp
                    )
                }
            ) {
                Text(if (isEditMode) "수정 완료" else "트리거 생성")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

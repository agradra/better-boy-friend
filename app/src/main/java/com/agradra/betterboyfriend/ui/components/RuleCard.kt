package com.agradra.betterboyfriend.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agradra.betterboyfriend.data.model.RuleItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RuleCard(
    rule: RuleItem,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeColor = rule.type.getComposePrimaryColor()
    val typeContainerColor = rule.type.getComposeContainerColor()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, typeColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 아이콘 (유형별 이모지 및 배경)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = typeContainerColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = rule.type.iconEmoji,
                        fontSize = 19.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 2. 태그 및 내용
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "#${rule.category}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = typeColor
                )
                Text(
                    text = rule.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 3. 레벨 (Lv.1, Lv.2, Lv.3)
            PriorityIndicator(priority = rule.priority)
        }
    }
}

@Composable
fun PriorityIndicator(priority: Int) {
    val badgeColor = when (priority) {
        3 -> Color(0xFFD32F2F)
        2 -> Color(0xFFF57C00)
        else -> Color(0xFF1976D2)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeColor.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        repeat(priority.coerceIn(1, 3)) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(11.dp)
            )
        }
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = "Lv.$priority",
            color = badgeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

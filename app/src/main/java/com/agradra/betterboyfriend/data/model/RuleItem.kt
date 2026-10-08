package com.agradra.betterboyfriend.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rule_items")
data class RuleItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: RuleType,
    val title: String,
    val priority: Int, // 1 (보통), 2 (중요), 3 (매우 중요/치명적)
    val category: String,
    val date: String, // YYYY-MM-DD
    val reason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

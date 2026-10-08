package com.agradra.betterboyfriend.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trigger_items")
data class TriggerItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val conditions: List<TriggerConditionType>,
    val category: String,
    val targetPerson: String = "",
    val targetApp: String = "",
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

package com.agradra.betterboyfriend.data.model

import androidx.compose.ui.graphics.Color

enum class RuleType(
    val displayName: String,
    val shortName: String,
    val iconEmoji: String,
    val primaryColor: Long,
    val containerColor: Long
) {
    DO(
        displayName = "하면 좋은 것",
        shortName = "Good",
        iconEmoji = "🥰",
        primaryColor = 0xFF1976D2,
        containerColor = 0xFFE3F2FD
    ),
    DONT(
        displayName = "하면 안 되는 것",
        shortName = "Bad",
        iconEmoji = "😡",
        primaryColor = 0xFFC62828,
        containerColor = 0xFFFFEBEE
    ),
    ASK(
        displayName = "하기 전 물어볼 것",
        shortName = "Ask",
        iconEmoji = "🤔",
        primaryColor = 0xFFF57F17,
        containerColor = 0xFFFFF9C4
    );

    fun getComposePrimaryColor(): Color = Color(primaryColor)
    fun getComposeContainerColor(): Color = Color(containerColor)

    companion object {
        fun fromString(value: String): RuleType {
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || 
                it.displayName == value || 
                it.shortName.equals(value, ignoreCase = true) 
            } ?: DO
        }
    }
}

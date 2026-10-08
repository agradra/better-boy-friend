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
        displayName = "Good",
        shortName = "Good",
        iconEmoji = "🥰",
        primaryColor = 0xFF1976D2,
        containerColor = 0xFFE3F2FD
    ),
    DONT(
        displayName = "Bad",
        shortName = "Bad",
        iconEmoji = "😡",
        primaryColor = 0xFFC62828,
        containerColor = 0xFFFFEBEE
    ),
    ASK(
        displayName = "Ask",
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

package com.agradra.betterboyfriend.data.model

enum class TriggerConditionType(
    val displayName: String,
    val shortName: String,
    val iconEmoji: String,
    val description: String
) {
    CALL_RECEIVED(
        displayName = "특정 사람한테 전화가 올 때",
        shortName = "전화 수신",
        iconEmoji = "📞",
        description = "특정 사람에게서 전화가 걸려올 때"
    ),
    MESSAGE_RECEIVED(
        displayName = "특정 사람한테 디엠/카톡/메시지가 올 때",
        shortName = "메시지/카톡",
        iconEmoji = "💬",
        description = "특정 사람에게서 카톡, 디엠, 문자가 올 때"
    ),
    APP_OPENED(
        displayName = "특정 앱을 켤 때",
        shortName = "앱 실행",
        iconEmoji = "📱",
        description = "카톡, 배민 등 특정 앱을 화면에 실행할 때"
    );

    companion object {
        fun fromString(value: String): TriggerConditionType? {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        }
    }
}

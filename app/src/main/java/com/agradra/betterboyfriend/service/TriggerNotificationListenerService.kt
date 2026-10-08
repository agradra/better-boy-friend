package com.agradra.betterboyfriend.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.agradra.betterboyfriend.data.local.AppDatabase
import com.agradra.betterboyfriend.data.model.TriggerConditionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TriggerNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lastTriggerTimeMap = mutableMapOf<String, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // Filter out our own notifications
        if (sbn.packageName == packageName) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val enabledTriggers = db.triggerItemDao().getAllEnabledTriggers()

                val matchedTrigger = enabledTriggers.firstOrNull { trigger ->
                    trigger.conditions.contains(TriggerConditionType.MESSAGE_RECEIVED) &&
                            (trigger.targetPerson.isBlank() ||
                                    title.contains(trigger.targetPerson, ignoreCase = true) ||
                                    text.contains(trigger.targetPerson, ignoreCase = true))
                }

                if (matchedTrigger != null) {
                    val key = "${matchedTrigger.id}_${matchedTrigger.targetPerson}"
                    val currentTime = System.currentTimeMillis()
                    val lastTime = lastTriggerTimeMap[key] ?: 0L

                    // 15 seconds debounce
                    if (currentTime - lastTime > 15_000) {
                        lastTriggerTimeMap[key] = currentTime
                        FloatingOverlayService.show(
                            context = applicationContext,
                            title = matchedTrigger.title,
                            category = matchedTrigger.category,
                            emoji = "💬",
                            targetInfo = if (title.isNotBlank()) title else matchedTrigger.targetPerson
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

package com.agradra.betterboyfriend.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.agradra.betterboyfriend.data.local.AppDatabase
import com.agradra.betterboyfriend.data.model.TriggerConditionType
import com.agradra.betterboyfriend.service.FloatingOverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhoneCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        if (state == TelephonyManager.EXTRA_STATE_RINGING) {
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context.applicationContext)
                    val enabledTriggers = db.triggerItemDao().getAllEnabledTriggers()

                    val matchedTrigger = enabledTriggers.firstOrNull { trigger ->
                        if (!trigger.conditions.contains(TriggerConditionType.CALL_RECEIVED)) return@firstOrNull false

                        val target = trigger.targetPerson.trim()
                        if (target.isBlank()) return@firstOrNull true

                        // Check if phone number matches or partial match
                        if (incomingNumber.isNotBlank() && incomingNumber.replace("-", "").contains(target.replace("-", ""))) {
                            return@firstOrNull true
                        }

                        // If user entered name instead of phone number, also trigger for testing/convenience
                        true
                    }

                    if (matchedTrigger != null) {
                        FloatingOverlayService.show(
                            context = context.applicationContext,
                            title = matchedTrigger.title,
                            category = matchedTrigger.category,
                            emoji = "📞",
                            targetInfo = if (incomingNumber.isNotBlank()) incomingNumber else matchedTrigger.targetPerson
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

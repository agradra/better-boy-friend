package com.agradra.betterboyfriend.service

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.agradra.betterboyfriend.data.local.AppDatabase
import com.agradra.betterboyfriend.data.model.TriggerConditionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TriggerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lastTriggerTimeMap = mutableMapOf<String, Long>()
    private var lastPackageName: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkgName = event.packageName?.toString() ?: return

        // Ignore our own app, launcher, system UI, keyboards
        if (pkgName == packageName ||
            pkgName.contains("launcher", ignoreCase = true) ||
            pkgName.contains("systemui", ignoreCase = true) ||
            pkgName.contains("inputmethod", ignoreCase = true)
        ) return

        // Avoid re-triggering if user is just navigating within the same app
        if (pkgName == lastPackageName) return
        lastPackageName = pkgName

        serviceScope.launch {
            try {
                // Get human-readable app label
                val appLabel = try {
                    val appInfo = packageManager.getApplicationInfo(pkgName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    pkgName
                }

                val db = AppDatabase.getDatabase(applicationContext)
                val enabledTriggers = db.triggerItemDao().getAllEnabledTriggers()

                val matchedTrigger = enabledTriggers.firstOrNull { trigger ->
                    if (!trigger.conditions.contains(TriggerConditionType.APP_OPENED)) return@firstOrNull false
                    matchesApp(trigger.targetApp, appLabel, pkgName)
                }

                if (matchedTrigger != null) {
                    val key = "${matchedTrigger.id}_$pkgName"
                    val currentTime = System.currentTimeMillis()
                    val lastTime = lastTriggerTimeMap[key] ?: 0L

                    // 20 seconds debounce for app launches
                    if (currentTime - lastTime > 20_000) {
                        lastTriggerTimeMap[key] = currentTime
                        FloatingOverlayService.show(
                            context = applicationContext,
                            title = matchedTrigger.title,
                            category = matchedTrigger.category,
                            emoji = "📱",
                            targetInfo = appLabel
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun matchesApp(targetInput: String, appLabel: String, pkgName: String): Boolean {
        val target = targetInput.trim().lowercase()
        if (target.isBlank()) return false

        val labelLower = appLabel.lowercase()
        val pkgLower = pkgName.lowercase()

        // 1. 직접 포함 검사 (영문명, 소문자, 부분 일치)
        if (labelLower.contains(target) || pkgLower.contains(target)) return true

        // 2. 한국어 별칭 및 주요 앱 키워드 매핑
        val aliasMap = mapOf(
            "유튜브" to listOf("youtube"),
            "유투브" to listOf("youtube"),
            "youtube" to listOf("youtube"),
            "카톡" to listOf("kakao", "talk"),
            "카카오톡" to listOf("kakao", "talk"),
            "인스타" to listOf("instagram"),
            "인스타그램" to listOf("instagram"),
            "페북" to listOf("facebook", "fb"),
            "페이스북" to listOf("facebook", "fb"),
            "배민" to listOf("baemin", "smartbaemin", "woowa"),
            "배달의민족" to listOf("baemin", "smartbaemin", "woowa"),
            "요기요" to listOf("yogiyo"),
            "쿠팡" to listOf("coupang"),
            "쿠팡이츠" to listOf("coupang.mobile.eats", "eats"),
            "당근" to listOf("daangn"),
            "당근마켓" to listOf("daangn"),
            "토스" to listOf("toss"),
            "네이버" to listOf("naver"),
            "넷플릭스" to listOf("netflix"),
            "트위터" to listOf("twitter"),
            "엑스" to listOf("twitter"),
            "갤러리" to listOf("gallery", "sec.android.gallery3d"),
            "사진" to listOf("gallery", "photos")
        )

        // 사용자가 입력한 검색어가 별칭 사전에 매칭되는지 확인
        for ((key, keywords) in aliasMap) {
            if (target.contains(key) || key.contains(target)) {
                if (keywords.any { labelLower.contains(it) || pkgLower.contains(it) }) {
                    return true
                }
            }
        }

        return false
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

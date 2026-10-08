package com.agradra.betterboyfriend.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.agradra.betterboyfriend.MainActivity
import com.agradra.betterboyfriend.R
import com.agradra.betterboyfriend.data.local.AppDatabase
import com.agradra.betterboyfriend.data.model.RuleItem
import com.agradra.betterboyfriend.data.model.RuleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RuleAppWidgetProvider : AppWidgetProvider() {

    private val providerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_SELECT_TYPE -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                val typeStr = intent.getStringExtra(EXTRA_TYPE)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && typeStr != null) {
                    val type = try {
                        RuleType.valueOf(typeStr)
                    } catch (e: Exception) {
                        RuleType.DONT
                    }
                    saveSelectedType(context, appWidgetId, type)
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateWidget(context, appWidgetManager, appWidgetId)
                }
            }
            ACTION_REFRESH_WIDGET -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, RuleAppWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                for (id in appWidgetIds) {
                    updateWidget(context, appWidgetManager, id)
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (id in appWidgetIds) {
            editor.remove(KEY_PREFIX_TYPE + id)
        }
        editor.apply()
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        providerScope.launch {
            try {
                val selectedType = getSavedType(context, appWidgetId)
                val db = AppDatabase.getDatabase(context)
                val allRules = db.ruleItemDao().getAllRules()

                // Filter by selected rule type and sort by priority descending
                val filteredRules = allRules
                    .filter { it.type == selectedType }
                    .sortedWith(
                        compareByDescending<RuleItem> { it.priority }
                            .thenByDescending { it.id }
                    )

                val views = RemoteViews(context.packageName, R.layout.widget_rule)

                // 1. 헤더 (상태 표시)
                val statusColor = when (selectedType) {
                    RuleType.DONT -> Color.parseColor("#FF453A")
                    RuleType.DO -> Color.parseColor("#0A84FF")
                    RuleType.ASK -> Color.parseColor("#FFD60A")
                }
                views.setTextViewText(R.id.widget_status, "${selectedType.name} • ${filteredRules.size}개")
                views.setTextColor(R.id.widget_status, statusColor)

                // 2. 리스트뷰 바인딩 (Android 12+ RemoteCollectionItems)
                val collectionBuilder = RemoteViews.RemoteCollectionItems.Builder()
                filteredRules.forEach { rule ->
                    val itemViews = RemoteViews(context.packageName, R.layout.widget_rule_item)
                    itemViews.setTextViewText(R.id.item_title, rule.title)
                    itemViews.setTextViewText(R.id.item_category, rule.category.ifBlank { "일반" })

                    val dotRes = when (rule.priority) {
                        3 -> R.drawable.widget_dot_red
                        2 -> R.drawable.widget_dot_orange
                        else -> R.drawable.widget_dot_blue
                    }
                    itemViews.setImageViewResource(R.id.item_dot, dotRes)
                    collectionBuilder.addItem(rule.id, itemViews)
                }

                views.setRemoteAdapter(R.id.widget_list_view, collectionBuilder.build())
                views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_text)
                views.setTextViewText(
                    R.id.widget_empty_text,
                    "등록된 ${selectedType.name} 지침이 없습니다"
                )

                // 3. 앱 열기 버튼 (왼쪽)
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val openPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_open_app, openPendingIntent)
                views.setPendingIntentTemplate(R.id.widget_list_view, openPendingIntent)

                // 4. 오른쪽 탭 버튼들 (DO, DONT, ASK)
                val isDo = selectedType == RuleType.DO
                views.setInt(
                    R.id.btn_type_do,
                    "setBackgroundResource",
                    if (isDo) R.drawable.widget_btn_selected_do else R.drawable.widget_btn_unselected
                )
                views.setTextColor(
                    R.id.btn_type_do,
                    if (isDo) Color.WHITE else Color.parseColor("#8E8E93")
                )
                views.setOnClickPendingIntent(
                    R.id.btn_type_do,
                    createTabPendingIntent(context, appWidgetId, RuleType.DO)
                )

                val isDont = selectedType == RuleType.DONT
                views.setInt(
                    R.id.btn_type_dont,
                    "setBackgroundResource",
                    if (isDont) R.drawable.widget_btn_selected_dont else R.drawable.widget_btn_unselected
                )
                views.setTextColor(
                    R.id.btn_type_dont,
                    if (isDont) Color.WHITE else Color.parseColor("#8E8E93")
                )
                views.setOnClickPendingIntent(
                    R.id.btn_type_dont,
                    createTabPendingIntent(context, appWidgetId, RuleType.DONT)
                )

                val isAsk = selectedType == RuleType.ASK
                views.setInt(
                    R.id.btn_type_ask,
                    "setBackgroundResource",
                    if (isAsk) R.drawable.widget_btn_selected_ask else R.drawable.widget_btn_unselected
                )
                views.setTextColor(
                    R.id.btn_type_ask,
                    if (isAsk) Color.BLACK else Color.parseColor("#8E8E93")
                )
                views.setOnClickPendingIntent(
                    R.id.btn_type_ask,
                    createTabPendingIntent(context, appWidgetId, RuleType.ASK)
                )

                // 위젯 업데이트 적용
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createTabPendingIntent(
        context: Context,
        appWidgetId: Int,
        type: RuleType
    ): PendingIntent {
        val intent = Intent(context, RuleAppWidgetProvider::class.java).apply {
            action = ACTION_SELECT_TYPE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_TYPE, type.name)
        }
        val requestCode = appWidgetId * 10 + type.ordinal
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_SELECT_TYPE = "com.agradra.betterboyfriend.ACTION_SELECT_TYPE"
        const val ACTION_REFRESH_WIDGET = "com.agradra.betterboyfriend.ACTION_REFRESH_WIDGET"
        const val EXTRA_TYPE = "extra_type"

        private const val PREFS_NAME = "rule_widget_prefs"
        private const val KEY_PREFIX_TYPE = "widget_type_"

        fun getSavedType(context: Context, appWidgetId: Int): RuleType {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val name = prefs.getString(KEY_PREFIX_TYPE + appWidgetId, RuleType.DONT.name)
            return try {
                RuleType.valueOf(name ?: RuleType.DONT.name)
            } catch (e: Exception) {
                RuleType.DONT
            }
        }

        fun saveSelectedType(context: Context, appWidgetId: Int, type: RuleType) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_PREFIX_TYPE + appWidgetId, type.name).apply()
        }

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, RuleAppWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            context.sendBroadcast(intent)
        }
    }
}

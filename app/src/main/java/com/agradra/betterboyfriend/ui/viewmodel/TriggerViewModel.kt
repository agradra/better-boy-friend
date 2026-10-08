package com.agradra.betterboyfriend.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agradra.betterboyfriend.data.local.AppDatabase
import com.agradra.betterboyfriend.data.model.TriggerConditionType
import com.agradra.betterboyfriend.data.model.TriggerItem
import com.agradra.betterboyfriend.data.repository.RuleRepository
import com.agradra.betterboyfriend.data.repository.TriggerRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TriggerViewModel(application: Application) : AndroidViewModel(application) {

    private val triggerRepository: TriggerRepository
    private val ruleRepository: RuleRepository

    init {
        val database = AppDatabase.getDatabase(application)
        triggerRepository = TriggerRepository(database.triggerItemDao())
        ruleRepository = RuleRepository(database.ruleItemDao())
    }

    val triggers: StateFlow<List<TriggerItem>> = triggerRepository.allTriggersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val availableCategories: StateFlow<List<String>> = ruleRepository.categoriesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    fun addTrigger(
        title: String,
        conditions: List<TriggerConditionType>,
        category: String,
        targetPerson: String = "",
        targetApp: String = ""
    ) {
        if (conditions.isEmpty()) {
            viewModelScope.launch {
                _toastEvent.emit("최소 하나 이상의 조건을 선택해주세요.")
            }
            return
        }

        viewModelScope.launch {
            val autoTitle = if (title.isBlank()) {
                val condNames = conditions.joinToString(", ") { it.shortName }
                "[$condNames] #$category"
            } else title.trim()

            val item = TriggerItem(
                title = autoTitle,
                conditions = conditions,
                category = category.trim().ifBlank { "일반" },
                targetPerson = targetPerson.trim(),
                targetApp = targetApp.trim(),
                isEnabled = true
            )
            triggerRepository.insert(item)
            _toastEvent.emit("트리거가 등록되었습니다.")
        }
    }

    fun updateTrigger(trigger: TriggerItem) {
        viewModelScope.launch {
            triggerRepository.update(trigger)
            _toastEvent.emit("트리거가 수정되었습니다.")
        }
    }

    fun deleteTrigger(trigger: TriggerItem) {
        viewModelScope.launch {
            triggerRepository.delete(trigger)
            _toastEvent.emit("트리거가 삭제되었습니다.")
        }
    }

    fun toggleTrigger(trigger: TriggerItem, isEnabled: Boolean) {
        viewModelScope.launch {
            triggerRepository.toggleEnabled(trigger.id, isEnabled)
            val stateText = if (isEnabled) "활성화" else "비활성화"
            _toastEvent.emit("'${trigger.title}' 트리거가 ${stateText}되었습니다.")
        }
    }
}

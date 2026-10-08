package com.agradra.betterboyfriend.data.repository

import com.agradra.betterboyfriend.data.local.TriggerItemDao
import com.agradra.betterboyfriend.data.model.TriggerItem
import kotlinx.coroutines.flow.Flow

class TriggerRepository(private val dao: TriggerItemDao) {

    val allTriggersFlow: Flow<List<TriggerItem>> = dao.getAllTriggersFlow()

    suspend fun insert(trigger: TriggerItem): Long {
        return dao.insertTrigger(trigger)
    }

    suspend fun update(trigger: TriggerItem) {
        dao.updateTrigger(trigger)
    }

    suspend fun delete(trigger: TriggerItem) {
        dao.deleteTrigger(trigger)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteTriggerById(id)
    }

    suspend fun toggleEnabled(id: Long, isEnabled: Boolean) {
        dao.updateEnabledState(id, isEnabled)
    }

    suspend fun getEnabledTriggers(): List<TriggerItem> {
        return dao.getAllEnabledTriggers()
    }
}

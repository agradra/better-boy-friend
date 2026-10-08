package com.agradra.betterboyfriend.data.repository

import com.agradra.betterboyfriend.data.local.RuleItemDao
import com.agradra.betterboyfriend.data.model.RuleItem
import kotlinx.coroutines.flow.Flow

class RuleRepository(private val dao: RuleItemDao) {

    val allRulesFlow: Flow<List<RuleItem>> = dao.getAllRulesFlow()
    val categoriesFlow: Flow<List<String>> = dao.getDistinctCategoriesFlow()

    suspend fun getAllRules(): List<RuleItem> {
        return dao.getAllRules()
    }

    suspend fun getRuleById(id: Long): RuleItem? {
        return dao.getRuleById(id)
    }

    suspend fun insert(rule: RuleItem): Long {
        return dao.insertRule(rule)
    }

    suspend fun update(rule: RuleItem) {
        dao.updateRule(rule)
    }

    suspend fun delete(rule: RuleItem) {
        dao.deleteRule(rule)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteRuleById(id)
    }

    suspend fun importFromCsv(importedRules: List<RuleItem>): Int {
        val existingRules = dao.getAllRules()
        val existingById = existingRules.associateBy { it.id }
        val existingByTitleAndType = existingRules.associateBy { "${it.type.name}_${it.title.trim().lowercase()}" }

        var count = 0
        for (item in importedRules) {
            val key = "${item.type.name}_${item.title.trim().lowercase()}"
            val existing = if (item.id > 0 && existingById.containsKey(item.id)) {
                existingById[item.id]
            } else {
                existingByTitleAndType[key]
            }

            if (existing != null) {
                // Update existing item with imported values
                dao.updateRule(
                    existing.copy(
                        type = item.type,
                        title = item.title,
                        priority = item.priority,
                        category = item.category.ifBlank { existing.category },
                        date = item.date.ifBlank { existing.date },
                        reason = item.reason.ifBlank { existing.reason }
                    )
                )
            } else {
                // Insert as new item (id = 0 so Room auto-generates)
                dao.insertRule(item.copy(id = 0))
            }
            count++
        }
        return count
    }
}

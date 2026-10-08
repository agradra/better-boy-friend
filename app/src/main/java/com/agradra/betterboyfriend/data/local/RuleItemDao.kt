package com.agradra.betterboyfriend.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.agradra.betterboyfriend.data.model.RuleItem
import com.agradra.betterboyfriend.data.model.RuleType
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleItemDao {

    @Query("SELECT * FROM rule_items ORDER BY priority DESC, date DESC, id DESC")
    fun getAllRulesFlow(): Flow<List<RuleItem>>

    @Query("SELECT * FROM rule_items ORDER BY priority DESC, date DESC, id DESC")
    suspend fun getAllRules(): List<RuleItem>

    @Query("SELECT * FROM rule_items WHERE type = :type ORDER BY priority DESC, date DESC, id DESC")
    fun getRulesByTypeFlow(type: RuleType): Flow<List<RuleItem>>

    @Query("SELECT * FROM rule_items WHERE id = :id LIMIT 1")
    suspend fun getRuleById(id: Long): RuleItem?

    @Query("SELECT DISTINCT category FROM rule_items WHERE category != '' ORDER BY category ASC")
    fun getDistinctCategoriesFlow(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(item: RuleItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(items: List<RuleItem>): List<Long>

    @Update
    suspend fun updateRule(item: RuleItem)

    @Delete
    suspend fun deleteRule(item: RuleItem)

    @Query("DELETE FROM rule_items WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    @Query("DELETE FROM rule_items")
    suspend fun deleteAllRules()
}

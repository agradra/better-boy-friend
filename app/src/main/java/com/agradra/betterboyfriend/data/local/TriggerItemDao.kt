package com.agradra.betterboyfriend.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.agradra.betterboyfriend.data.model.TriggerItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TriggerItemDao {

    @Query("SELECT * FROM trigger_items ORDER BY id DESC")
    fun getAllTriggersFlow(): Flow<List<TriggerItem>>

    @Query("SELECT * FROM trigger_items WHERE isEnabled = 1")
    suspend fun getAllEnabledTriggers(): List<TriggerItem>

    @Query("SELECT * FROM trigger_items WHERE id = :id LIMIT 1")
    suspend fun getTriggerById(id: Long): TriggerItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrigger(item: TriggerItem): Long

    @Update
    suspend fun updateTrigger(item: TriggerItem)

    @Delete
    suspend fun deleteTrigger(item: TriggerItem)

    @Query("DELETE FROM trigger_items WHERE id = :id")
    suspend fun deleteTriggerById(id: Long)

    @Query("UPDATE trigger_items SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateEnabledState(id: Long, isEnabled: Boolean)
}

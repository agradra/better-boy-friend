package com.agradra.betterboyfriend.data.local

import androidx.room.TypeConverter
import com.agradra.betterboyfriend.data.model.RuleType

class Converters {
    @TypeConverter
    fun fromRuleType(type: RuleType?): String {
        return type?.name ?: RuleType.DO.name
    }

    @TypeConverter
    fun toRuleType(value: String?): RuleType {
        return if (value != null) RuleType.fromString(value) else RuleType.DO
    }

    @TypeConverter
    fun fromTriggerConditions(conditions: List<com.agradra.betterboyfriend.data.model.TriggerConditionType>?): String {
        return conditions?.joinToString(",") { it.name } ?: ""
    }

    @TypeConverter
    fun toTriggerConditions(value: String?): List<com.agradra.betterboyfriend.data.model.TriggerConditionType> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").mapNotNull { 
            com.agradra.betterboyfriend.data.model.TriggerConditionType.fromString(it.trim()) 
        }
    }
}

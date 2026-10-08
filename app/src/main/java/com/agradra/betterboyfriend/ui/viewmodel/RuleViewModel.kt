package com.agradra.betterboyfriend.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agradra.betterboyfriend.data.local.AppDatabase
import com.agradra.betterboyfriend.data.model.RuleItem
import com.agradra.betterboyfriend.data.model.RuleType
import com.agradra.betterboyfriend.data.repository.RuleRepository
import com.agradra.betterboyfriend.util.CsvHelper
import com.agradra.betterboyfriend.widget.RuleAppWidgetProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class SortOption(val displayName: String) {
    PRIORITY_DESC("중요도 높은순"),
    DATE_DESC("최신 등록순"),
    DATE_ASC("오래된순"),
    TITLE_ASC("가나다순")
}

data class RuleUiState(
    val rules: List<RuleItem> = emptyList(),
    val totalCount: Int = 0,
    val doCount: Int = 0,
    val dontCount: Int = 0,
    val askCount: Int = 0,
    val categories: List<String> = emptyList()
)

class RuleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RuleRepository

    init {
        val dao = AppDatabase.getDatabase(application).ruleItemDao()
        repository = RuleRepository(dao)
    }

    private val _selectedFilterType = MutableStateFlow<RuleType?>(null)
    val selectedFilterType: StateFlow<RuleType?> = _selectedFilterType.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.PRIORITY_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private data class FilterState(
        val filterType: RuleType? = null,
        val category: String? = null,
        val query: String = "",
        val sort: SortOption = SortOption.PRIORITY_DESC
    )

    private val filterState = combine(
        _selectedFilterType,
        _selectedCategory,
        _searchQuery,
        _sortOption
    ) { filterType, category, query, sort ->
        FilterState(filterType, category, query, sort)
    }

    val uiState: StateFlow<RuleUiState> = combine(
        repository.allRulesFlow,
        repository.categoriesFlow,
        filterState
    ) { allRules, categories, filters ->
        val filtered = allRules.filter { rule ->
            val matchType = filters.filterType == null || rule.type == filters.filterType
            val matchCategory = filters.category == null || rule.category == filters.category
            val matchQuery = filters.query.isBlank() || 
                rule.title.contains(filters.query, ignoreCase = true) || 
                rule.reason.contains(filters.query, ignoreCase = true) ||
                rule.category.contains(filters.query, ignoreCase = true)
            matchType && matchCategory && matchQuery
        }.let { list ->
            when (filters.sort) {
                SortOption.PRIORITY_DESC -> list.sortedWith(
                    compareByDescending<RuleItem> { it.priority }
                        .thenByDescending { it.date }
                        .thenByDescending { it.id }
                )
                SortOption.DATE_DESC -> list.sortedWith(
                    compareByDescending<RuleItem> { it.date }
                        .thenByDescending { it.priority }
                )
                SortOption.DATE_ASC -> list.sortedWith(
                    compareBy<RuleItem> { it.date }
                        .thenByDescending { it.priority }
                )
                SortOption.TITLE_ASC -> list.sortedBy { it.title }
            }
        }

        RuleUiState(
            rules = filtered,
            totalCount = allRules.size,
            doCount = allRules.count { it.type == RuleType.DO },
            dontCount = allRules.count { it.type == RuleType.DONT },
            askCount = allRules.count { it.type == RuleType.ASK },
            categories = categories
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RuleUiState()
    )

    fun setFilterType(type: RuleType?) {
        _selectedFilterType.value = type
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun addRule(
        type: RuleType,
        title: String,
        priority: Int,
        category: String,
        date: String,
        reason: String
    ) {
        viewModelScope.launch {
            val formattedDate = if (date.isBlank()) getTodayDateString() else date
            val item = RuleItem(
                type = type,
                title = title.trim(),
                priority = priority.coerceIn(1, 3),
                category = category.trim().ifBlank { "일반" },
                date = formattedDate,
                reason = reason.trim()
            )
            repository.insert(item)
            RuleAppWidgetProvider.updateAllWidgets(getApplication())
            _toastEvent.emit("항목이 추가되었습니다.")
        }
    }

    fun updateRule(rule: RuleItem) {
        viewModelScope.launch {
            repository.update(rule)
            RuleAppWidgetProvider.updateAllWidgets(getApplication())
            _toastEvent.emit("수정되었습니다.")
        }
    }

    fun deleteRule(rule: RuleItem) {
        viewModelScope.launch {
            repository.delete(rule)
            RuleAppWidgetProvider.updateAllWidgets(getApplication())
            _toastEvent.emit("삭제되었습니다.")
        }
    }

    fun exportToCsv(outputStream: OutputStream) {
        viewModelScope.launch {
            try {
                val rules = repository.getAllRules()
                CsvHelper.exportToCsv(rules, outputStream)
                _toastEvent.emit("CSV로 ${rules.size}개 항목을 내보냈습니다.")
            } catch (e: Exception) {
                e.printStackTrace()
                _toastEvent.emit("CSV 내보내기 실패: ${e.localizedMessage}")
            }
        }
    }

    fun importFromCsv(inputStream: InputStream) {
        viewModelScope.launch {
            try {
                val parsedItems = CsvHelper.importFromCsv(inputStream)
                if (parsedItems.isEmpty()) {
                    _toastEvent.emit("가져올 유효한 데이터가 없습니다.")
                    return@launch
                }
                val count = repository.importFromCsv(parsedItems)
                RuleAppWidgetProvider.updateAllWidgets(getApplication())
                _toastEvent.emit("CSV에서 ${count}개 항목을 성공적으로 반영했습니다.")
            } catch (e: Exception) {
                e.printStackTrace()
                _toastEvent.emit("CSV 가져오기 실패: ${e.localizedMessage}")
            }
        }
    }

    fun getTodayDateString(): String {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}

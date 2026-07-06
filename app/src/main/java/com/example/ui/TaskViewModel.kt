package com.planca.app.ui

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.planca.app.data.Task
import com.planca.app.data.TaskRepository
import com.planca.app.util.LogHelper
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class TaskFilter {
    ALL,
    ACTIVE,
    COMPLETED
}

class TaskViewModel(
    private val application: Application,
    private val repository: TaskRepository
) : ViewModel() {

    private val prefs = application.getSharedPreferences("planca_prefs", Context.MODE_PRIVATE)

    private fun updateAllWidgets() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(application)
            val classes = listOf(
                "com.planca.app.PlancaWidgetSmallProvider",
                "com.planca.app.PlancaWidgetMediumProvider",
                "com.planca.app.PlancaWidgetLargeProvider"
            )
            for (className in classes) {
                val component = ComponentName(application, className)
                val ids = appWidgetManager.getAppWidgetIds(component)
                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                    setComponent(component)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                application.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            LogHelper.e("Failed to update widgets", e)
        }
    }

    private val _currentFilter = MutableStateFlow(TaskFilter.ALL)
    val currentFilter: StateFlow<TaskFilter> = _currentFilter

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedDate = MutableStateFlow<Calendar?>(null)
    val selectedDate: StateFlow<Calendar?> = _selectedDate

    fun setSelectedDate(date: Calendar?) {
        _selectedDate.value = date
    }

    private fun parseTaskDueDate(dueDateStr: String): Calendar? {
        if (dueDateStr.isBlank()) return null
        val formats = listOf(
            "d MMMM yyyy",
            "dd MMMM yyyy",
            "d MMM yyyy"
        )
        val locales = listOf(java.util.Locale.forLanguageTag("tr"), java.util.Locale.ENGLISH, java.util.Locale.getDefault())
        for (fmt in formats) {
            for (loc in locales) {
                try {
                    val sdf = java.text.SimpleDateFormat(fmt, loc)
                    sdf.isLenient = false
                    val date = sdf.parse(dueDateStr)
                    if (date != null) {
                        return Calendar.getInstance().apply { time = date }
                    }
                } catch (e: Exception) {
                }
            }
        }
        return null
    }

    private fun isSameDay(c1: Calendar, c2: Calendar): Boolean {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
               c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    // Available categories
    private val _categories = MutableStateFlow(loadCategories())
    val categories: StateFlow<List<String>> = _categories

    // Category colors map
    private val _categoryColors = MutableStateFlow(loadCategoryColors())
    val categoryColors: StateFlow<Map<String, String>> = _categoryColors

    private fun loadCategories(): List<String> {
        val saved = prefs.getString("custom_categories_list", null)
        if (saved == null) return listOf("Kişisel", "İş", "Sağlık", "Okul", "Alışveriş")
        return try {
            val jsonArray = JSONArray(saved)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            listOf("Kişisel", "İş", "Sağlık", "Okul", "Alışveriş")
        }
    }

    private fun loadCategoryColors(): Map<String, String> {
        val saved = prefs.getString("custom_category_colors_map", null)
        if (saved == null) return mapOf(
            "Kişisel" to "#E1BEE7",
            "İş" to "#BBDEFB",
            "Sağlık" to "#C8E6C9",
            "Okul" to "#FFE0B2",
            "Alışveriş" to "#F8BBD0"
        )
        return try {
            val jsonObject = JSONObject(saved)
            val map = mutableMapOf<String, String>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.getString(key)
            }
            map
        } catch (e: Exception) {
            mapOf(
                "Kişisel" to "#E1BEE7",
                "İş" to "#BBDEFB",
                "Sağlık" to "#C8E6C9",
                "Okul" to "#FFE0B2",
                "Alışveriş" to "#F8BBD0"
            )
        }
    }

    private fun saveCategories() {
        val jsonArray = JSONArray(_categories.value)
        prefs.edit().putString("custom_categories_list", jsonArray.toString()).apply()
        
        val jsonObject = JSONObject()
        _categoryColors.value.forEach { (k, v) -> jsonObject.put(k, v) }
        prefs.edit().putString("custom_category_colors_map", jsonObject.toString()).apply()
    }

    fun getCategoryColor(category: String?, isDark: Boolean): String {
        if (category.isNullOrBlank()) return if (isDark) "#2A2D35" else "#F5F5F5"
        val lightColor = _categoryColors.value[category] ?: run {
            val fallbacks = listOf("#E1BEE7", "#BBDEFB", "#C8E6C9", "#FFE0B2", "#F8BBD0", "#B2DFDB", "#FFF9C4")
            val index = Math.abs(category.hashCode()) % fallbacks.size
            fallbacks[index]
        }
        
        if (!isDark) return lightColor
        
        return when (lightColor.uppercase()) {
            "#E1BEE7" -> "#CE93D8" // Lavender (darker/richer in dark mode)
            "#BBDEFB" -> "#90CAF9" // Soft Blue
            "#C8E6C9" -> "#A5D6A7" // Soft Green
            "#FFE0B2" -> "#FFCC80" // Soft Peach
            "#F8BBD0" -> "#F48FB1" // Soft Pink
            "#B2DFDB" -> "#80CBC4" // Soft Teal / Mint
            "#FFF9C4" -> "#FFF59D" // Soft Yellow
            else -> lightColor
        }
    }

    val unfilteredTasks: StateFlow<List<Task>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val tasks: StateFlow<List<Task>> = combine(
        repository.allTasks, 
        _currentFilter, 
        _selectedCategory, 
        _searchQuery,
        _selectedDate
    ) { allTasks, filter, category, query, selDate ->
        val filtered = when (filter) {
            TaskFilter.ALL -> allTasks
            TaskFilter.ACTIVE -> allTasks.filter { !it.isCompleted }
            TaskFilter.COMPLETED -> allTasks.filter { it.isCompleted }
        }
        
        val dateFiltered = if (selDate == null) {
            filtered
        } else {
            filtered.filter { task ->
                val taskDate = parseTaskDueDate(task.dueDate)
                // If task has no due date, show it on the current real today if we are viewing today
                // OR if it matches the selected date. 
                // Let's keep it simple: if a task has no date, it's "today"
                if (taskDate == null) {
                    val today = Calendar.getInstance()
                    isSameDay(today, selDate)
                } else {
                    isSameDay(taskDate, selDate)
                }
            }
        }

        val categoryFiltered = if (category == null) {
            dateFiltered
        } else {
            dateFiltered.filter { it.category.equals(category, ignoreCase = true) }
        }
        if (query.isBlank()) {
            categoryFiltered
        } else {
            categoryFiltered.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.description.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun addCustomCategory(category: String, colorHex: String) {
        val trimmed = category.trim()
        if (trimmed.isNotBlank() && !_categories.value.contains(trimmed)) {
            if (_categories.value.size >= 20) return
            _categories.value = _categories.value + trimmed
            _categoryColors.value = _categoryColors.value + (trimmed to colorHex)
            saveCategories()
        }
    }

    fun deleteCategory(category: String) {
        viewModelScope.launch {
            unfilteredTasks.value.forEach { task ->
                if (task.category.equals(category, ignoreCase = true)) {
                    repository.update(task.copy(category = ""))
                }
            }
            _categories.value = _categories.value.filter { !it.equals(category, ignoreCase = true) }
            _categoryColors.value = _categoryColors.value.filterKeys { !it.equals(category, ignoreCase = true) }
            if (_selectedCategory.value.equals(category, ignoreCase = true)) {
                _selectedCategory.value = null
            }
            saveCategories()
            updateAllWidgets()
        }
    }

    fun moveCategory(fromIndex: Int, toIndex: Int) {
        val currentList = _categories.value
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val newList = currentList.toMutableList()
            val item = newList.removeAt(fromIndex)
            newList.add(toIndex, item)
            _categories.value = newList
            saveCategories()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addTask(title: String, description: String = "", category: String = "", dueDate: String = "", priority: Int = 3) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insert(
                Task(
                    title = title.trim(), 
                    description = description.trim(),
                    category = category.trim(),
                    dueDate = dueDate.trim(),
                    priority = priority
                )
            )
            updateAllWidgets()
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            repository.update(task.copy(isCompleted = !task.isCompleted))
            updateAllWidgets()
        }
    }

    fun updateTaskDetails(task: Task, title: String, description: String, category: String, dueDate: String, priority: Int) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.update(
                task.copy(
                    title = title.trim(), 
                    description = description.trim(),
                    category = category.trim(),
                    dueDate = dueDate.trim(),
                    priority = priority
                )
            )
            updateAllWidgets()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteById(task.id)
            updateAllWidgets()
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            val selected = _selectedCategory.value
            val selectedDateValue = _selectedDate.value
            
            if (selectedDateValue != null) {
                // Filter completed tasks for the selected day
                val all = repository.allTasks.stateIn(viewModelScope).value
                val locale = if (prefs.getString("language", "Türkçe") == "Türkçe") java.util.Locale.forLanguageTag("tr") else java.util.Locale.ENGLISH
                val dateFormat = java.text.SimpleDateFormat("d MMMM yyyy", locale)
                val dateStr = dateFormat.format(selectedDateValue.time)
                
                all.filter { it.isCompleted && (it.dueDate.startsWith(dateStr) || (it.dueDate.isBlank() && isSameDay(Calendar.getInstance(), selectedDateValue))) }
                   .forEach { repository.deleteById(it.id) }
            } else if (selected != null) {
                repository.clearCompletedByCategory(selected)
            } else {
                repository.clearCompleted()
            }
            updateAllWidgets()
        }
    }
}

class TaskViewModelFactory(
    private val application: Application,
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

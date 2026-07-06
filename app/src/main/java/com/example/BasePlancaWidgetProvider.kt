package com.planca.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.View
import android.widget.RemoteViews
import com.planca.app.data.Task
import com.planca.app.data.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

abstract class BasePlancaWidgetProvider(
    private val layoutResId: Int,
    private val sizeId: String
) : AppWidgetProvider() {

    companion object {
        const val ACTION_TASK_COMPLETE = "com.planca.app.ACTION_TASK_COMPLETE"
        const val ACTION_ADD_TASK = "com.planca.app.ACTION_ADD_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TASK_COMPLETE -> {
                val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
                if (taskId != -1) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val database = TaskDatabase.getDatabase(context)
                        val dao = database.taskDao()
                        val tasks = dao.getAllTasks().first()
                        val task = tasks.find { it.id == taskId }
                        if (task != null) {
                            dao.updateTask(task.copy(isCompleted = true))
                            updateAllWidgets(context)
                        }
                    }
                }
            }
            ACTION_ADD_TASK -> {
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("open_add_task", true)
                }
                context.startActivity(mainIntent)
            }
        }
    }

    private fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val classes = listOf(
            "com.planca.app.PlancaWidgetSmallProvider",
            "com.planca.app.PlancaWidgetMediumProvider",
            "com.planca.app.PlancaWidgetLargeProvider"
        )
        for (className in classes) {
            val component = ComponentName(context, className)
            val ids = appWidgetManager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val updateIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                    setComponent(component)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(updateIntent)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = TaskDatabase.getDatabase(context)
                val allTasks = database.taskDao().getAllTasks().first()
                val prefs = context.getSharedPreferences("planca_prefs", Context.MODE_PRIVATE)

                for (appWidgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, appWidgetId, allTasks, prefs)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        allTasks: List<Task>,
        prefs: SharedPreferences
    ) {
        val opacity = prefs.getFloat("widget_opacity", 0.9f)
        val themeId = prefs.getString("widget_theme", "modern_koyu") ?: "modern_koyu"
        val filterType = prefs.getString("widget_filter_type", "bugun") ?: "bugun"
        val selectedCategory = prefs.getString("widget_filter_category", "İş") ?: "İş"
        val showAddButton = prefs.getBoolean("widget_show_add_button", true)
        val currentLanguage = prefs.getString("app_language", "Türkçe") ?: "Türkçe"
        val isTr = currentLanguage == "Türkçe"

        // Filter tasks for today or category
        val today = Calendar.getInstance()
        fun isSameDay(c1: Calendar, c2: Calendar): Boolean {
            return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                   c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
        }
        
        fun parseTaskDueDate(dueDateStr: String): Calendar? {
            if (dueDateStr.isBlank()) return null
            val cleanStr = dueDateStr.replace(Regex("\\s+"), " ").trim()
            val formats = listOf(
                "d MMMM yyyy, HH:mm", "dd MMMM yyyy, HH:mm", "d MMM yyyy, HH:mm",
                "d MMMM yyyy HH:mm", "dd MMMM yyyy HH:mm", "d MMM yyyy HH:mm",
                "d MMMM yyyy", "dd MMMM yyyy", "d MMM yyyy", "HH:mm"
            )
            val locales = listOf(Locale("tr"), Locale.ENGLISH, Locale.getDefault())
            for (fmt in formats) {
                for (loc in locales) {
                    try {
                        val sdf = SimpleDateFormat(fmt, loc)
                        sdf.isLenient = true
                        val date = sdf.parse(cleanStr)
                        if (date != null) {
                            val cal = Calendar.getInstance()
                            val targetCal = Calendar.getInstance().apply { time = date }
                            if (fmt == "HH:mm") {
                                targetCal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE))
                            } else if (!fmt.contains("HH:mm")) {
                                targetCal.set(Calendar.HOUR_OF_DAY, 8)
                                targetCal.set(Calendar.MINUTE, 0)
                            }
                            return targetCal
                        }
                    } catch (e: Exception) {}
                }
            }
            return null
        }

        val filteredTasks = allTasks.filter { task ->
            val taskDate = parseTaskDueDate(task.dueDate)
            val isToday = if (taskDate == null) {
                // Task with no date is considered "Today" if it's not completed
                // but user wants all tasks for today.
                true 
            } else isSameDay(taskDate, today)
            
            if (filterType == "kategori") {
                isToday && task.category.equals(selectedCategory, ignoreCase = true)
            } else {
                isToday
            }
        }.sortedWith(compareBy<Task> { it.isCompleted }.thenBy { it.priority })

        // Themes configuration
        val widgetThemes = mapOf(
            "modern_koyu" to WidgetThemeColors("#0F172A", "#FFFFFF", "#94A3B8", "#38BDF8"),
            "klasik_beyaz" to WidgetThemeColors("#FFFFFF", "#0F172A", "#64748B", "#2563EB"),
            "neon_mavi" to WidgetThemeColors("#020617", "#38BDF8", "#0284C7", "#00F5FF"),
            "doga_yesili" to WidgetThemeColors("#064E3B", "#ECFDF5", "#A7F3D0", "#10B981"),
            "pastel_pembe" to WidgetThemeColors("#FFF1F2", "#881337", "#FB7185", "#EC4899")
        )

        val categoryColorsMap = mapOf(
            "İş" to "#BBDEFB",
            "Kişisel" to "#E1BEE7",
            "Sağlık" to "#C8E6C9",
            "Okul" to "#FFE0B2",
            "Alışveriş" to "#F8BBD0"
        )

        val themeColors = widgetThemes[themeId] ?: widgetThemes["modern_koyu"]!!
        val bgResolved = parseColor(themeColors.bgHex, opacity)
        val textPrimaryResolved = parseColorOnly(themeColors.textPrimaryHex)
        val textSecondaryResolved = parseColorOnly(themeColors.textSecondaryHex)
        val accentResolved = parseColorOnly(themeColors.accentHex)

        val views = RemoteViews(context.packageName, layoutResId)

        // Root background
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background)
            views.setColorStateList(R.id.widget_root, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(bgResolved))
        } else {
            // Fallback for older versions: use a simple color if rounding causes issues
            views.setInt(R.id.widget_root, "setBackgroundColor", bgResolved)
        }

        // App launch intent
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        // Add task intent
        val addIntent = Intent(context, this::class.java).apply {
            action = ACTION_ADD_TASK
        }
        val addPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId + 200,
            addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Date Sdf
        val dateSdf = if (isTr) SimpleDateFormat("d MMMM", Locale("tr")) else SimpleDateFormat("MMMM d", Locale.US)
        val formattedDate = dateSdf.format(Date())

        when (layoutResId) {
            R.layout.widget_small -> {
                views.setTextViewText(R.id.widget_date, formattedDate)
                views.setInt(R.id.widget_date, "setTextColor", accentResolved)

                views.setTextViewText(R.id.widget_counter, filteredTasks.size.toString())
                views.setInt(R.id.widget_counter, "setTextColor", textPrimaryResolved)

                views.setTextViewText(R.id.widget_counter_label, if (isTr) "Görev" else "Tasks")
                views.setInt(R.id.widget_counter_label, "setTextColor", textSecondaryResolved)

                if (filteredTasks.isEmpty()) {
                    views.setViewVisibility(R.id.widget_tasks_container, View.GONE)
                    views.setViewVisibility(R.id.widget_empty_text, View.VISIBLE)
                    views.setTextViewText(R.id.widget_empty_text, if (isTr) "Planlanmış görev yok" else "No scheduled tasks")
                    views.setInt(R.id.widget_empty_text, "setTextColor", textSecondaryResolved)
                } else {
                    views.setViewVisibility(R.id.widget_tasks_container, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_empty_text, View.GONE)

                    // Task Row 1
                    if (filteredTasks.isNotEmpty()) {
                        val task = filteredTasks[0]
                        views.setViewVisibility(R.id.widget_task_row_1, View.VISIBLE)
                        views.setTextViewText(R.id.widget_task_1, task.title)
                        views.setInt(R.id.widget_task_1, "setTextColor", if (task.isCompleted) textSecondaryResolved else textPrimaryResolved)
                        val catColor = parseColorOnly(categoryColorsMap[task.category] ?: themeColors.accentHex)
                        views.setTextColor(R.id.widget_dot_1, if (task.isCompleted) textSecondaryResolved else catColor)
                        if (task.isCompleted) {
                            views.setTextViewText(R.id.widget_dot_1, "✓")
                        } else {
                            views.setTextViewText(R.id.widget_dot_1, "•")
                        }
                    } else {
                        views.setViewVisibility(R.id.widget_task_row_1, View.GONE)
                    }

                    // Task Row 2
                    if (filteredTasks.size >= 2) {
                        val task = filteredTasks[1]
                        views.setViewVisibility(R.id.widget_task_row_2, View.VISIBLE)
                        views.setTextViewText(R.id.widget_task_2, task.title)
                        views.setInt(R.id.widget_task_2, "setTextColor", if (task.isCompleted) textSecondaryResolved else textPrimaryResolved)
                        val catColor = parseColorOnly(categoryColorsMap[task.category] ?: themeColors.accentHex)
                        views.setTextColor(R.id.widget_dot_2, if (task.isCompleted) textSecondaryResolved else catColor)
                        if (task.isCompleted) {
                            views.setTextViewText(R.id.widget_dot_2, "✓")
                        } else {
                            views.setTextViewText(R.id.widget_dot_2, "•")
                        }
                    } else {
                        views.setViewVisibility(R.id.widget_task_row_2, View.GONE)
                    }
                }
            }
            R.layout.widget_medium -> {
                val daySdf = if (isTr) SimpleDateFormat("EEEE", Locale("tr")) else SimpleDateFormat("EEEE", Locale.US)
                val dayName = daySdf.format(Date()).uppercase()

                views.setTextViewText(R.id.widget_day_name, dayName)
                views.setInt(R.id.widget_day_name, "setTextColor", textSecondaryResolved)

                views.setTextViewText(R.id.widget_date, formattedDate)
                views.setInt(R.id.widget_date, "setTextColor", textPrimaryResolved)

                views.setTextViewText(R.id.widget_count_badge, if (isTr) "${filteredTasks.size} Görev" else "${filteredTasks.size} Tasks")
                views.setInt(R.id.widget_count_badge, "setTextColor", accentResolved)

                if (filteredTasks.isEmpty()) {
                    views.setViewVisibility(R.id.widget_tasks_container, View.GONE)
                    views.setViewVisibility(R.id.widget_empty_text, View.VISIBLE)
                    views.setTextViewText(R.id.widget_empty_text, if (isTr) "Bugün hiç görev yok" else "No tasks today")
                    views.setInt(R.id.widget_empty_text, "setTextColor", textSecondaryResolved)
                } else {
                    views.setViewVisibility(R.id.widget_tasks_container, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_empty_text, View.GONE)

                    // Task Row 1
                    if (filteredTasks.isNotEmpty()) {
                        val task = filteredTasks[0]
                        views.setViewVisibility(R.id.widget_task_row_1, View.VISIBLE)
                        views.setTextViewText(R.id.widget_task_1, task.title)
                        views.setInt(R.id.widget_task_1, "setTextColor", if (task.isCompleted) textSecondaryResolved else textPrimaryResolved)
                        
                        val checkRes = if (task.isCompleted) R.drawable.ic_widget_check_on else R.drawable.ic_widget_check_off
                        views.setImageViewResource(R.id.widget_check_1, checkRes)
                        setImageViewColorFilter(views, R.id.widget_check_1, if (task.isCompleted) accentResolved else textSecondaryResolved)
                        
                        val catColor = parseColorOnly(categoryColorsMap[task.category] ?: themeColors.accentHex)
                        views.setTextColor(R.id.widget_dot_1, if (task.isCompleted) textSecondaryResolved else catColor)

                        val completeIntent = Intent(context, this::class.java).apply {
                            action = ACTION_TASK_COMPLETE
                            putExtra(EXTRA_TASK_ID, task.id)
                        }
                        val completePendingIntent = PendingIntent.getBroadcast(
                            context,
                            task.id,
                            completeIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_check_1, completePendingIntent)
                    } else {
                        views.setViewVisibility(R.id.widget_task_row_1, View.GONE)
                    }

                    // Task Row 2
                    if (filteredTasks.size >= 2) {
                        val task = filteredTasks[1]
                        views.setViewVisibility(R.id.widget_task_row_2, View.VISIBLE)
                        views.setTextViewText(R.id.widget_task_2, task.title)
                        views.setInt(R.id.widget_task_2, "setTextColor", if (task.isCompleted) textSecondaryResolved else textPrimaryResolved)
                        
                        val checkRes = if (task.isCompleted) R.drawable.ic_widget_check_on else R.drawable.ic_widget_check_off
                        views.setImageViewResource(R.id.widget_check_2, checkRes)
                        setImageViewColorFilter(views, R.id.widget_check_2, if (task.isCompleted) accentResolved else textSecondaryResolved)
                        
                        val catColor = parseColorOnly(categoryColorsMap[task.category] ?: themeColors.accentHex)
                        views.setTextColor(R.id.widget_dot_2, if (task.isCompleted) textSecondaryResolved else catColor)

                        val completeIntent = Intent(context, this::class.java).apply {
                            action = ACTION_TASK_COMPLETE
                            putExtra(EXTRA_TASK_ID, task.id)
                        }
                        val completePendingIntent = PendingIntent.getBroadcast(
                            context,
                            task.id + 1000,
                            completeIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_check_2, completePendingIntent)
                    } else {
                        views.setViewVisibility(R.id.widget_task_row_2, View.GONE)
                    }

                    // Task Row 3
                    if (filteredTasks.size >= 3) {
                        val task = filteredTasks[2]
                        views.setViewVisibility(R.id.widget_task_row_3, View.VISIBLE)
                        views.setTextViewText(R.id.widget_task_3, task.title)
                        views.setInt(R.id.widget_task_3, "setTextColor", if (task.isCompleted) textSecondaryResolved else textPrimaryResolved)
                        
                        val checkRes = if (task.isCompleted) R.drawable.ic_widget_check_on else R.drawable.ic_widget_check_off
                        views.setImageViewResource(R.id.widget_check_3, checkRes)
                        setImageViewColorFilter(views, R.id.widget_check_3, if (task.isCompleted) accentResolved else textSecondaryResolved)
                        
                        val catColor = parseColorOnly(categoryColorsMap[task.category] ?: themeColors.accentHex)
                        views.setTextColor(R.id.widget_dot_3, if (task.isCompleted) textSecondaryResolved else catColor)

                        val completeIntent = Intent(context, this::class.java).apply {
                            action = ACTION_TASK_COMPLETE
                            putExtra(EXTRA_TASK_ID, task.id)
                        }
                        val completePendingIntent = PendingIntent.getBroadcast(
                            context,
                            task.id + 2000,
                            completeIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_check_3, completePendingIntent)
                    } else {
                        views.setViewVisibility(R.id.widget_task_row_3, View.GONE)
                    }
                }

                if (showAddButton) {
                    views.setViewVisibility(R.id.widget_add_button, View.VISIBLE)
                    setImageViewColorFilter(views, R.id.widget_add_button, if (themeId == "modern_koyu") android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                    views.setInt(R.id.widget_add_button, "setBackgroundColor", accentResolved)
                    views.setOnClickPendingIntent(R.id.widget_add_button, addPendingIntent)
                } else {
                    views.setViewVisibility(R.id.widget_add_button, View.GONE)
                }
            }
            R.layout.widget_large -> {
                views.setTextViewText(R.id.widget_header_title, if (filterType == "kategori") selectedCategory.uppercase() else (if (isTr) "BUGÜNÜN GÖREVLERİ" else "TODAY'S TASKS"))
                views.setInt(R.id.widget_header_title, "setTextColor", accentResolved)

                val fullDaySdf = if (isTr) SimpleDateFormat("EEEE, d MMMM", Locale("tr")) else SimpleDateFormat("EEEE, MMMM d", Locale.US)
                views.setTextViewText(R.id.widget_header_date, fullDaySdf.format(Date()))
                views.setInt(R.id.widget_header_date, "setTextColor", textPrimaryResolved)

                views.setTextViewText(R.id.widget_count_badge, if (isTr) "${filteredTasks.size} Görev" else "${filteredTasks.size} Tasks")
                views.setInt(R.id.widget_count_badge, "setTextColor", textSecondaryResolved)

                if (filteredTasks.isEmpty()) {
                    views.setViewVisibility(R.id.widget_tasks_container, View.GONE)
                    views.setViewVisibility(R.id.widget_empty_text, View.VISIBLE)
                    views.setTextViewText(R.id.widget_empty_text, if (isTr) "Bugün hiç görev yok" else "No tasks today")
                    views.setInt(R.id.widget_empty_text, "setTextColor", textSecondaryResolved)
                } else {
                    views.setViewVisibility(R.id.widget_tasks_container, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_empty_text, View.GONE)

                    val maxRows = 5
                    for (i in 0 until maxRows) {
                        val rowId = context.resources.getIdentifier("widget_task_row_${i + 1}", "id", context.packageName)
                        val textId = context.resources.getIdentifier("widget_task_${i + 1}", "id", context.packageName)
                        val checkId = context.resources.getIdentifier("widget_check_${i + 1}", "id", context.packageName)
                        val dotId = context.resources.getIdentifier("widget_dot_${i + 1}", "id", context.packageName)

                        if (rowId != 0 && textId != 0 && checkId != 0 && dotId != 0) {
                            if (i < filteredTasks.size) {
                                val task = filteredTasks[i]
                                views.setViewVisibility(rowId, View.VISIBLE)
                                views.setTextViewText(textId, task.title)
                                views.setInt(textId, "setTextColor", if (task.isCompleted) textSecondaryResolved else textPrimaryResolved)
                                
                                val checkRes = if (task.isCompleted) R.drawable.ic_widget_check_on else R.drawable.ic_widget_check_off
                                views.setImageViewResource(checkId, checkRes)
                                setImageViewColorFilter(views, checkId, if (task.isCompleted) accentResolved else textSecondaryResolved)
                                
                                val catColor = parseColorOnly(categoryColorsMap[task.category] ?: themeColors.accentHex)
                                views.setTextColor(dotId, if (task.isCompleted) textSecondaryResolved else catColor)

                                val completeIntent = Intent(context, this::class.java).apply {
                                    action = ACTION_TASK_COMPLETE
                                    putExtra(EXTRA_TASK_ID, task.id)
                                }
                                val completePendingIntent = PendingIntent.getBroadcast(
                                    context,
                                    task.id + 3000 + i,
                                    completeIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                views.setOnClickPendingIntent(checkId, completePendingIntent)
                            } else {
                                views.setViewVisibility(rowId, View.GONE)
                            }
                        }
                    }
                }

                if (showAddButton) {
                    views.setViewVisibility(R.id.widget_footer_bar, View.VISIBLE)
                    views.setTextViewText(R.id.widget_add_label, if (isTr) "Yeni Görev Ekle" else "Add New Task")
                    views.setInt(R.id.widget_add_label, "setTextColor", textSecondaryResolved)
                    setImageViewColorFilter(views, R.id.widget_add_button, accentResolved)
                    views.setOnClickPendingIntent(R.id.widget_footer_bar, addPendingIntent)
                } else {
                    views.setViewVisibility(R.id.widget_footer_bar, View.GONE)
                }
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }


    private fun setImageViewColorFilter(views: RemoteViews, viewId: Int, color: Int) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            views.setColorStateList(viewId, "setImageTintList", android.content.res.ColorStateList.valueOf(color))
        } else {
            views.setInt(viewId, "setColorFilter", color)
        }
    }

    private fun parseColor(hex: String, opacity: Float): Int {
        val colorInt = android.graphics.Color.parseColor(hex)
        val alpha = (opacity * 255).toInt()
        return (alpha shl 24) or (colorInt and 0x00FFFFFF)
    }

    private fun parseColorOnly(hex: String): Int {
        return android.graphics.Color.parseColor(hex)
    }

    private data class WidgetThemeColors(
        val bgHex: String,
        val textPrimaryHex: String,
        val textSecondaryHex: String,
        val accentHex: String
    )
}

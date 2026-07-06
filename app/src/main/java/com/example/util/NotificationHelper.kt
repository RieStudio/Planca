package com.planca.app.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.planca.app.MainActivity
import com.planca.app.TaskReminderReceiver
import com.planca.app.data.Task
import java.text.SimpleDateFormat
import java.util.*

object NotificationHelper {
    private const val CHANNEL_ID = "planca_tasks_channel"
    private const val CHANNEL_NAME = "Planca Tasks"
    private const val CHANNEL_DESC = "Notifications for Planca tasks and reminders"

    fun parseTaskDueDate(dueDateStr: String): Long? {
        if (dueDateStr.isBlank()) return null
        
        val cleanStr = dueDateStr.replace(Regex("\\s+"), " ").trim()
        
        val formats = listOf(
            "d MMMM yyyy, HH:mm",
            "dd MMMM yyyy, HH:mm",
            "d MMM yyyy, HH:mm",
            "d MMMM yyyy HH:mm",
            "dd MMMM yyyy HH:mm",
            "d MMM yyyy HH:mm",
            "d MMMM yyyy",
            "dd MMMM yyyy",
            "d MMM yyyy",
            "HH:mm"
        )
        val locales = listOf(Locale.forLanguageTag("tr"), Locale.ENGLISH, Locale.getDefault())
        
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
                            targetCal.set(Calendar.YEAR, cal.get(Calendar.YEAR))
                            targetCal.set(Calendar.MONTH, cal.get(Calendar.MONTH))
                            targetCal.set(Calendar.DATE, cal.get(Calendar.DATE))
                        } else if (!fmt.contains("HH:mm")) {
                            targetCal.set(Calendar.HOUR_OF_DAY, 8)
                            targetCal.set(Calendar.MINUTE, 0)
                        }
                        
                        return targetCal.timeInMillis
                    }
                } catch (e: Exception) {}
            }
        }
        return null
    }

    fun scheduleTaskReminder(context: Context, task: Task) {
        val prefs = context.getSharedPreferences("planca_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications", true)
        if (!notificationsEnabled) return
        
        val minutesBefore = prefs.getInt("notification_time_before", 15)
        val dueDateStr = task.dueDate ?: ""
        val targetTimeMs = parseTaskDueDate(dueDateStr) ?: return
        
        val reminderTimeMs = targetTimeMs - (minutesBefore * 60 * 1000)
        
        if (reminderTimeMs <= System.currentTimeMillis()) return
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra("task_id", task.id)
            putExtra("task_title", task.title)
            putExtra("minutes_before", minutesBefore)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMs, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTimeMs, pendingIntent)
            }
        } catch (e: Exception) {
            // Log error in production
        }
    }
    
    fun cancelTaskReminder(context: Context, taskId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendNotification(context: Context, title: String, content: String, notificationId: Int = System.currentTimeMillis().toInt()) {
        val prefs = context.getSharedPreferences("planca_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications", true)
        if (!notificationsEnabled) return

        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val smallIcon = android.R.drawable.ic_dialog_info

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
}

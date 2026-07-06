package com.planca.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.planca.app.util.NotificationHelper

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("task_id", -1)
        val taskTitle = intent.getStringExtra("task_title") ?: "Görev"
        val minutesBefore = intent.getIntExtra("minutes_before", 15)
        
        val isTr = context.getSharedPreferences("planca_prefs", Context.MODE_PRIVATE)
            .getString("language", "Türkçe") == "Türkçe"
            
        val title = if (isTr) "Görev Yaklaşıyor! ⏰" else "Task Approaching! ⏰"
        
        val timeLabel = if (minutesBefore >= 60 && minutesBefore % 60 == 0) {
            val hours = minutesBefore / 60
            if (isTr) "$hours saat" else "$hours hour${if (hours > 1) "s" else ""}"
        } else {
            if (isTr) "$minutesBefore dakika" else "$minutesBefore minute${if (minutesBefore > 1) "s" else ""}"
        }

        val content = if (isTr) {
            if (minutesBefore > 0) "\"$taskTitle\" görevinize $timeLabel kaldı!"
            else "\"$taskTitle\" görevinin vakti geldi!"
        } else {
            if (minutesBefore > 0) "Only $timeLabel left for your task \"$taskTitle\"!"
            else "It's time for your task \"$taskTitle\"!"
        }
        
        NotificationHelper.sendNotification(context, title, content, taskId)
    }
}

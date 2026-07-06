package com.planca.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.planca.app.ui.components.AppText
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PlancaDatePickerDialog(
    initialDate: Calendar,
    isTr: Boolean,
    onDateSelected: (Calendar) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDate by remember { mutableStateOf(initialDate.clone() as Calendar) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.width(320.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                AppText(
                    text = if (isTr) "Tarih Seçin" else "Select Date",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val locale = if (isTr) Locale.forLanguageTag("tr") else Locale.ENGLISH
                AppText(
                    text = SimpleDateFormat("EEE, d MMM", locale).format(selectedDate.time),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        selectedDate = (selectedDate.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                    }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
                    }
                    
                    AppText(
                        text = SimpleDateFormat("MMMM yyyy", locale).format(selectedDate.time),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = {
                        selectedDate = (selectedDate.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                    }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val daysInMonth = selectedDate.getActualMaximum(Calendar.DAY_OF_MONTH)
                val firstDayOfMonth = (selectedDate.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK)
                val emptySlots = if (firstDayOfMonth == Calendar.SUNDAY) 6 else firstDayOfMonth - 2
                
                Column {
                    val daysOfWeek = if (isTr) listOf("P", "S", "Ç", "P", "C", "C", "P") else listOf("M", "T", "W", "T", "F", "S", "S")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        daysOfWeek.forEach { day ->
                            AppText(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var currentDay = 1
                    for (i in 0..5) {
                        if (currentDay > daysInMonth) break
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (j in 0..6) {
                                val slotIndex = i * 7 + j
                                if (slotIndex < emptySlots || currentDay > daysInMonth) {
                                    Spacer(modifier = Modifier.weight(1f))
                                } else {
                                    val day = currentDay
                                    val isSelected = selectedDate.get(Calendar.DAY_OF_MONTH) == day
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .clickable {
                                                selectedDate = (selectedDate.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AppText(
                                            text = day.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    currentDay++
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        AppText(if (isTr) "İptal" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onDateSelected(selectedDate) }) {
                        AppText(if (isTr) "Seç" else "Select")
                    }
                }
            }
        }
    }
}

@Composable
fun PlancaTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    isTr: Boolean,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.width(300.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppText(
                    text = if (isTr) "Saat Seçin" else "Select Time",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NumberPicker(
                        value = selectedHour,
                        range = 0..23,
                        onValueChange = { selectedHour = it }
                    )
                    
                    AppText(
                        text = ":",
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    NumberPicker(
                        value = selectedMinute,
                        range = 0..59,
                        onValueChange = { selectedMinute = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        AppText(if (isTr) "İptal" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onTimeSelected(selectedHour, selectedMinute) }) {
                        AppText(if (isTr) "Seç" else "Select")
                    }
                }
            }
        }
    }
}

@Composable
fun NumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = {
            val next = if (value + 1 > range.last) range.first else value + 1
            onValueChange(next)
        }) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
        }
        
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), 
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            AppText(
                text = String.format(Locale.US, "%02d", value),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        IconButton(onClick = {
            val prev = if (value - 1 < range.first) range.last else value - 1
            onValueChange(prev)
        }) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
        }
    }
}

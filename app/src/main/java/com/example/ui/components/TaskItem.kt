package com.planca.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.planca.app.data.Task
import com.planca.app.getCategoryDisplayName
import com.planca.app.ui.dialogs.PlancaDatePickerDialog
import com.planca.app.ui.dialogs.PlancaTimePickerDialog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskRow(
    task: Task,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (String, String, Int, String, String) -> Unit,
    getCategoryColor: (String?) -> String,
    categories: List<String>,
    isTr: Boolean,
    modifier: Modifier = Modifier
) {
    var titleInput by remember(task.id) { mutableStateOf(task.title) }
    var descInput by remember(task.id) { mutableStateOf(task.description) }
    var priorityInput by remember(task.id) { mutableIntStateOf(task.priority) }
    var categoryInput by remember(task.id) { mutableStateOf(task.category ?: "") }
    
    val initialDate = remember(task.dueDate) { 
        if (task.dueDate.contains(",")) task.dueDate.split(",").first().trim()
        else if (task.dueDate.contains(" ") && !task.dueDate.contains(":")) task.dueDate.trim()
        else ""
    }
    val initialTime = remember(task.dueDate) {
        if (task.dueDate.contains(",")) task.dueDate.split(",").last().trim()
        else if (task.dueDate.contains(":") && !task.dueDate.contains(",")) task.dueDate.trim()
        else ""
    }
    
    var dateInput by remember(task.id) { mutableStateOf(initialDate) }
    var timeInput by remember(task.id) { mutableStateOf(initialTime) }

    var showCustomDatePicker by remember { mutableStateOf(false) }
    var showCustomTimePicker by remember { mutableStateOf(false) }

    if (showCustomDatePicker) {
        PlancaDatePickerDialog(
            initialDate = if (dateInput.isBlank()) Calendar.getInstance() else {
                try {
                    val cal = Calendar.getInstance()
                    val locale = if (isTr) Locale.forLanguageTag("tr") else Locale.ENGLISH
                    cal.time = SimpleDateFormat("d MMMM yyyy", locale).parse(dateInput)!!
                    cal
                } catch (e: Exception) { Calendar.getInstance() }
            },
            isTr = isTr,
            onDateSelected = { selCal: Calendar ->
                val locale = if (isTr) Locale.forLanguageTag("tr") else Locale.ENGLISH
                dateInput = SimpleDateFormat("d MMMM yyyy", locale).format(selCal.time)
                val formatted = when {
                    dateInput.isNotBlank() && timeInput.isNotBlank() -> "$dateInput, $timeInput"
                    dateInput.isNotBlank() -> dateInput
                    timeInput.isNotBlank() -> timeInput
                    else -> ""
                }
                onUpdate(titleInput, descInput, priorityInput, categoryInput, formatted)
                showCustomDatePicker = false
            },
            onDismiss = { showCustomDatePicker = false }
        )
    }

    if (showCustomTimePicker) {
        PlancaTimePickerDialog(
            initialHour = try { timeInput.split(":")[0].toInt() } catch (e: Exception) { 9 },
            initialMinute = try { timeInput.split(":")[1].toInt() } catch (e: Exception) { 0 },
            isTr = isTr,
            onTimeSelected = { h: Int, m: Int ->
                timeInput = String.format(Locale.US, "%02d:%02d", h, m)
                val formatted = when {
                    dateInput.isNotBlank() && timeInput.isNotBlank() -> "$dateInput, $timeInput"
                    dateInput.isNotBlank() -> dateInput
                    timeInput.isNotBlank() -> timeInput
                    else -> ""
                }
                onUpdate(titleInput, descInput, priorityInput, categoryInput, formatted)
                showCustomTimePicker = false
            },
            onDismiss = { showCustomTimePicker = false }
        )
    }
    
    val isCompleted = task.isCompleted
    val cardBgColor = if (isCompleted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val cardBorderColor = if (isCompleted) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
    }
    
    val checkScale by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "checkScale"
    )
    val checkboxBounce by animateFloatAsState(
        targetValue = if (isCompleted) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "checkboxBounce"
    )
    val haloScale by animateFloatAsState(
        targetValue = if (isCompleted) 1.8f else 1.0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "haloScale"
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (isCompleted) 0f else 0.4f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "haloAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardBgColor)
            .border(
                width = 1.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onToggleExpand
            )
            .padding(18.dp)
            .testTag("task_item_${task.id}")
            .alpha(if (isCompleted) 0.65f else 1.0f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .scale(checkboxBounce)
                    .align(Alignment.CenterVertically),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted && haloAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .scale(haloScale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = haloAlpha))
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            if (task.isCompleted) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .border(
                            width = 2.dp,
                            color = if (task.isCompleted) Color.Transparent else MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .clickable(onClick = onToggleComplete)
                        .testTag("task_checkbox_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Complete",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(13.dp)
                            .scale(checkScale)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                AppText(
                    text = task.title,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onBackground
                    )
                )
                
                if (task.description.isNotBlank() && !isExpanded) {
                    Spacer(modifier = Modifier.height(2.dp))
                    AppText(
                        text = task.description,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                if ((task.category.isNotBlank() || task.dueDate.isNotBlank()) && !isExpanded) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (task.category.isNotBlank()) {
                            val catColorHex = getCategoryColor(task.category)
                            val catColor = Color(android.graphics.Color.parseColor(catColorHex))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(catColor.copy(alpha = 0.85f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                AppText(
                                    text = getCategoryDisplayName(task.category, isTr).uppercase(Locale.getDefault()),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111318),
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                        if (task.dueDate.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(11.dp)
                                )
                                AppText(
                                    text = task.dueDate,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(14.dp))
                
                val currentFormattedDueDate = when {
                    dateInput.isNotBlank() && timeInput.isNotBlank() -> "$dateInput, $timeInput"
                    dateInput.isNotBlank() -> dateInput
                    timeInput.isNotBlank() -> timeInput
                    else -> ""
                }

                AppText(
                    text = if (isTr) "GÖREV BAŞLIĞI" else "TASK TITLE",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                AppTextField(
                    value = titleInput,
                    onValueChange = {
                        titleInput = it
                        onUpdate(titleInput, descInput, priorityInput, categoryInput, currentFormattedDueDate)
                    },
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.04f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp)
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                AppText(
                    text = if (isTr) "AÇIKLAMA / DETAYLAR" else "DESCRIPTION / DETAILS",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                AppTextField(
                    value = descInput,
                    onValueChange = {
                        descInput = it
                        onUpdate(titleInput, descInput, priorityInput, categoryInput, currentFormattedDueDate)
                    },
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.04f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp)
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                AppText(
                    text = if (isTr) "ÖNCELİK" else "PRIORITY",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { prio ->
                        val isSelected = priorityInput == prio
                        val bgCol = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        val borderCol = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        val textCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(bgCol)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = borderCol,
                                    shape = CircleShape
                                )
                                .clickable {
                                    priorityInput = prio
                                    onUpdate(titleInput, descInput, prio, categoryInput, currentFormattedDueDate)
                                }
                                .testTag("edit_priority_selector_$prio"),
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(
                                text = prio.toString(),
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textCol
                                )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                AppText(
                    text = if (isTr) "KATEGORİ" else "CATEGORY",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isCatSelected = categoryInput.equals(cat, ignoreCase = true)
                        val catColorHex = getCategoryColor(cat)
                        val catColor = Color(android.graphics.Color.parseColor(catColorHex))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCatSelected) catColor.copy(alpha = 0.85f)
                                    else catColor.copy(alpha = 0.15f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isCatSelected) catColor else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    val newCat = if (isCatSelected) "" else cat
                                    categoryInput = newCat
                                    onUpdate(titleInput, descInput, priorityInput, newCat, currentFormattedDueDate)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            AppText(
                                text = getCategoryDisplayName(cat, isTr),
                                fontSize = 11.sp,
                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSystemInDarkTheme()) {
                                    if (isCatSelected) Color(0xFF111318) else MaterialTheme.colorScheme.onBackground
                                } else {
                                    if (isCatSelected) Color.White else MaterialTheme.colorScheme.onBackground
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                AppText(
                    text = if (isTr) "TARİH VE SAAT" else "DATE AND TIME",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (dateInput.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.04f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (dateInput.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                showCustomDatePicker = true
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = if (dateInput.isBlank()) (if (isTr) "Tarih Seç" else "Set Date") else dateInput,
                            fontSize = 12.sp,
                            fontWeight = if (dateInput.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                            color = if (dateInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (timeInput.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.04f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (timeInput.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                showCustomTimePicker = true
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = if (timeInput.isBlank()) (if (isTr) "Saat Seç" else "Set Time") else timeInput,
                            fontSize = 12.sp,
                            fontWeight = if (timeInput.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                            color = if (timeInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Task",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        AppText(if (isTr) "Görevi Sil" else "Delete Task", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

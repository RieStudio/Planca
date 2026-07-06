package com.planca.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.planca.app.BuildConfig
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.planca.app.ui.components.AppText
import com.planca.app.ui.components.AppTextField
import com.planca.app.ui.dialogs.PlancaTimePickerDialog
import com.planca.app.util.LogHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TaskViewModel,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    notificationTimeBefore: Int,
    onNotificationTimeChange: (Int) -> Unit,
    onReplayTutorial: () -> Unit,
    onBack: () -> Unit
) {
    val isTr = currentLanguage == "Türkçe"
    val context = LocalContext.current
    var showMoreThemesDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showWidgetSetupScreen by remember { mutableStateOf(false) }
    var showHistoryScreen by remember { mutableStateOf(false) }

    if (showHistoryScreen) {
        HistoryScreen(
            viewModel = viewModel,
            isTr = isTr,
            onBack = { showHistoryScreen = false }
        )
    } else if (showWidgetSetupScreen) {
        WidgetSetupScreen(
            currentLanguage = currentLanguage,
            onBack = { showWidgetSetupScreen = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        AppText(
                            text = if (isTr) "Ayarlar" else "Settings",
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AppText(
                    text = if (isTr) "GENEL" else "GENERAL",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                )

                SettingsSelectionRow(
                    title = if (isTr) "Dil" else "Language",
                    icon = Icons.Default.Info,
                    options = listOf("Türkçe", "English"),
                    selectedOption = currentLanguage,
                    onOptionSelected = onLanguageChange
                )
                
                SettingsSelectionRow(
                    title = if (isTr) "Tema" else "Theme",
                    icon = Icons.Default.Face,
                    options = listOf(if (isTr) "Açık" else "Light", if (isTr) "Koyu" else "Dark"),
                    selectedOption = currentTheme,
                    onOptionSelected = onThemeChange,
                    moreOptionLabel = if (isTr) "Daha Fazla" else "More",
                    onMoreClick = { showMoreThemesDialog = true }
                )

                Spacer(modifier = Modifier.height(8.dp))
                AppText(
                    text = if (isTr) "BİLDİRİMLER" else "NOTIFICATIONS",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                )

                SettingsToggleItem(
                    title = if (isTr) "Bildirimleri Etkinleştir" else "Enable Notifications",
                    subtitle = if (isTr) "Görev hatırlatıcılarını al" else "Receive task reminders",
                    icon = Icons.Default.Notifications,
                    checked = notificationsEnabled,
                    onCheckedChange = onNotificationsChange
                )

                if (notificationsEnabled) {
                    val hours = notificationTimeBefore / 60
                    val minutes = notificationTimeBefore % 60
                    val timeLabel = when {
                        hours > 0 && minutes > 0 -> if (isTr) "$hours saat $minutes dakika önce" else "$hours hr $minutes min before"
                        hours > 0 -> if (isTr) "$hours saat önce" else "$hours hr before"
                        else -> if (isTr) "$minutes dakika önce" else "$minutes min before"
                    }
                    
                    SettingsItem(
                        title = if (isTr) "Hatırlatma Zamanı" else "Reminder Time",
                        subtitle = timeLabel,
                        icon = Icons.Default.Notifications,
                        onClick = { showTimePickerDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                AppText(
                    text = if (isTr) "ARAÇLAR" else "TOOLS",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                )

                SettingsItem(
                    title = if (isTr) "Widget Ayarları" else "Widget Settings",
                    subtitle = if (isTr) "Widgetları özelleştir" else "Customize widgets",
                    icon = Icons.Default.Build,
                    onClick = { showWidgetSetupScreen = true }
                )

                SettingsItem(
                    title = if (isTr) "Geçmiş" else "History",
                    subtitle = if (isTr) "Tamamlanan görevler" else "Completed tasks",
                    icon = Icons.Default.DateRange,
                    onClick = { showHistoryScreen = true }
                )

                SettingsItem(
                    title = if (isTr) "Hata & Görüş Bildir" else "Report Bug & Feedback",
                    subtitle = "contact@riestudio.com.tr",
                    icon = Icons.Default.Email,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:contact@riestudio.com.tr")
                            putExtra(Intent.EXTRA_SUBJECT, if (isTr) "Planca Uygulama Geri Bildirimi" else "Planca App Feedback")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback if no email client is installed
                        }
                    }
                )

                SettingsItem(
                    title = if (isTr) "Öğreticiyi Tekrar İzle" else "Replay Tutorial",
                    subtitle = if (isTr) "Uygulama kullanımını öğren" else "Learn how to use the app",
                    icon = Icons.Default.PlayArrow,
                    onClick = onReplayTutorial
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AppText(
                            text = "POWERED BY",
                            style = TextStyle(
                                fontSize = 12.sp, 
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        val isDarkTheme = currentTheme.lowercase() in listOf("koyu", "dark", "ultra dark", "neon", "ultra_dark") || 
                                         (currentTheme.lowercase() in listOf("sistem", "system") && isSystemInDarkTheme())
                        
                        val logoRes = if (isDarkTheme) com.planca.app.R.drawable.koyurs else com.planca.app.R.drawable.beyazrs
                        
                        Image(
                            painter = painterResource(id = logoRes),
                            contentDescription = "RieStudio Logo",
                            modifier = Modifier.height(80.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                    
                    AppText(
                        text = if (isTr) "Sürüm: ${BuildConfig.VERSION_NAME}" else "Version: ${BuildConfig.VERSION_NAME}",
                        style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)),
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (showTimePickerDialog) {
            PlancaTimePickerDialog(
                initialHour = notificationTimeBefore / 60,
                initialMinute = notificationTimeBefore % 60,
                isTr = isTr,
                onTimeSelected = { h, m ->
                    onNotificationTimeChange(h * 60 + m)
                    showTimePickerDialog = false
                },
                onDismiss = { showTimePickerDialog = false }
            )
        }


        if (showMoreThemesDialog) {
            AlertDialog(
                onDismissRequest = { showMoreThemesDialog = false },
                title = { AppText(if (isTr) "Tema Seç" else "Choose Theme") },
                text = {
                    Column {
                        val themes = listOf("Retro", "Neon", "Ultra Dark", "Nature", "Pembe")
                        themes.forEach { theme ->
                            TextButton(
                                onClick = {
                                    onThemeChange(theme)
                                    showMoreThemesDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                AppText(theme)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMoreThemesDialog = false }) {
                        AppText(if (isTr) "Kapat" else "Close")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsSelectionRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    moreOptionLabel: String? = null,
    onMoreClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            AppText(title, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp))
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = selectedOption.equals(option, ignoreCase = true)
                Button(
                    onClick = { onOptionSelected(option) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    AppText(option, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
            
            if (moreOptionLabel != null && onMoreClick != null) {
                OutlinedButton(
                    onClick = onMoreClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    AppText(moreOptionLabel, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            AppText(title, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp))
            AppText(subtitle, style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            AppText(title, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp))
            AppText(subtitle, style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

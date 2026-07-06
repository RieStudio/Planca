package com.planca.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planca.app.data.TaskDatabase
import com.planca.app.data.TaskRepository
import com.planca.app.ui.SettingsScreen
import com.planca.app.ui.TaskFilter
import com.planca.app.ui.TaskViewModel
import com.planca.app.ui.TaskViewModelFactory
import com.planca.app.ui.components.AppText
import com.planca.app.ui.components.AppTextField
import com.planca.app.ui.components.FilterTab
import com.planca.app.ui.components.PlancaIcon
import com.planca.app.ui.components.TaskRow
import com.planca.app.ui.dialogs.PlancaDatePickerDialog
import com.planca.app.ui.dialogs.PlancaTimePickerDialog
import com.planca.app.ui.onboarding.OnboardingTutorial
import com.planca.app.ui.theme.MyApplicationTheme
import com.planca.app.util.NotificationHelper
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlin.math.abs
import kotlin.math.roundToInt

fun getCategoryDisplayName(category: String?, isTr: Boolean): String {
    if (category.isNullOrBlank()) return ""
    if (isTr) return category
    return when (category) {
        "Kişisel" -> "Personal"
        "İş" -> "Work"
        "Sağlık" -> "Health"
        "Okul" -> "School"
        "Alışveriş" -> "Shopping"
        else -> category
    }
}

fun isColorDark(colorHex: String): Boolean {
    return try {
        val color = android.graphics.Color.parseColor(colorHex)
        val r = android.graphics.Color.red(color)
        val g = android.graphics.Color.green(color)
        val b = android.graphics.Color.blue(color)
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        luminance < 0.5
    } catch (e: Exception) {
        false
    }
}

class MainActivity : ComponentActivity() {
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermission()
        
        try {
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this)
            val providerClasses = listOf(
                "com.planca.app.PlancaWidgetSmallProvider",
                "com.planca.app.PlancaWidgetMediumProvider",
                "com.planca.app.PlancaWidgetLargeProvider"
            )
            for (className in providerClasses) {
                val component = android.content.ComponentName(this, className)
                val ids = appWidgetManager.getAppWidgetIds(component)
                if (ids.isNotEmpty()) {
                    val intent = android.content.Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                        setComponent(component)
                        putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    sendBroadcast(intent)
                }
            }
        } catch (e: Exception) {
            // Silently fail if widget update fails
        }
        
        val database = TaskDatabase.getDatabase(this)
        val repository = TaskRepository(database.taskDao())
        val viewModelFactory = TaskViewModelFactory(application, repository)
        
        setContent {
            val context = LocalContext.current
            val viewModel: TaskViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = viewModelFactory
            )
            val selectedViewDate by viewModel.selectedDate.collectAsStateWithLifecycle()

            val prefs = remember { context.getSharedPreferences("planca_prefs", MODE_PRIVATE) }
            val systemInDark = androidx.compose.foundation.isSystemInDarkTheme()
            var appThemePreference by remember { 
                val savedTheme = prefs.getString("theme", null)
                val initialTheme = if (savedTheme == null || savedTheme == "Sistem" || savedTheme == "System") {
                    val themeChoice = if (systemInDark) "Koyu" else "Açık"
                    prefs.edit().putString("theme", themeChoice).apply()
                    themeChoice
                } else {
                    savedTheme
                }
                mutableStateOf(initialTheme)
            }
            var appLanguagePreference by remember { 
                mutableStateOf(prefs.getString("language", "Türkçe") ?: "Türkçe") 
            }
            var appNotificationsEnabled by remember { 
                mutableStateOf(prefs.getBoolean("notifications", true)) 
            }
            var notificationTimeBefore by remember {
                mutableIntStateOf(prefs.getInt("notification_time_before", 15))
            }
            var isSettingsActive by remember { mutableStateOf(false) }
            var showTutorial by remember {
                mutableStateOf(prefs.getBoolean("is_first_run", true))
            }
            
            val isTr = appLanguagePreference == "Türkçe"
            val isDarkTheme = when (appThemePreference) {
                "Koyu", "Dark" -> true
                "Açık", "Light" -> false
                "Sistem", "System" -> systemInDark
                else -> {
                    appThemePreference.lowercase() in listOf("ultra dark", "ultra_dark", "neon")
                }
            }
            
            var showSplash by remember { mutableStateOf(true) }

            MyApplicationTheme(themePreference = appThemePreference, darkTheme = isDarkTheme) {
                if (showSplash) {
                    SplashScreen { showSplash = false }
                } else {
                    BackHandler(enabled = isSettingsActive) {
                        isSettingsActive = false
                    }
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Crossfade(
                                targetState = isSettingsActive,
                                animationSpec = tween(durationMillis = 150),
                                label = "screen_transition"
                            ) { active ->
                                if (active) {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        currentLanguage = appLanguagePreference,
                                        onLanguageChange = { 
                                            appLanguagePreference = it 
                                            prefs.edit().putString("language", it).apply()
                                        },
                                        currentTheme = appThemePreference,
                                        onThemeChange = { 
                                            appThemePreference = it 
                                            prefs.edit().putString("theme", it).apply()
                                        },
                                        notificationsEnabled = appNotificationsEnabled,
                                        onNotificationsChange = { 
                                            appNotificationsEnabled = it 
                                            prefs.edit().putBoolean("notifications", it).apply()
                                        },
                                        notificationTimeBefore = notificationTimeBefore,
                                        onNotificationTimeChange = {
                                            notificationTimeBefore = it
                                            prefs.edit().putInt("notification_time_before", it).apply()
                                        },
                                        onReplayTutorial = {
                                            isSettingsActive = false
                                            showTutorial = true
                                        },
                                        onBack = { isSettingsActive = false }
                                    )
                                } else {
                                    PlancaScreen(
                                        viewModel = viewModel,
                                        appLanguagePreference = appLanguagePreference,
                                        onSettingsClick = { isSettingsActive = true },
                                        isDarkTheme = isDarkTheme,
                                        appTheme = appThemePreference,
                                        appNotificationsEnabled = appNotificationsEnabled,
                                        appNotificationTimeBefore = notificationTimeBefore,
                                        selectedViewDate = selectedViewDate ?: Calendar.getInstance()
                                    )
                                }
                            }

                            if (showTutorial) {
                                OnboardingTutorial(
                                    isTr = isTr,
                                    onDismiss = {
                                        showTutorial = false
                                        prefs.edit().putBoolean("is_first_run", false).apply()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "splash_alpha"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1.05f else 0.85f,
        animationSpec = tween(durationMillis = 1000),
        label = "splash_scale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1800)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.koyurs),
            contentDescription = "Planca Splash Logo",
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer(
                    alpha = alphaAnim,
                    scaleX = scaleAnim,
                    scaleY = scaleAnim
                ),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlancaScreen(
    viewModel: TaskViewModel,
    appLanguagePreference: String,
    onSettingsClick: () -> Unit,
    isDarkTheme: Boolean,
    appTheme: String,
    appNotificationsEnabled: Boolean,
    appNotificationTimeBefore: Int,
    selectedViewDate: Calendar
) {
    val context = LocalContext.current
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val unfilteredTasks by viewModel.unfilteredTasks.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    
    LaunchedEffect(unfilteredTasks, appNotificationsEnabled, appNotificationTimeBefore) {
        unfilteredTasks.forEach { task ->
            if (appNotificationsEnabled && !task.isCompleted && task.dueDate.isNotBlank()) {
                NotificationHelper.scheduleTaskReminder(context, task)
            } else {
                NotificationHelper.cancelTaskReminder(context, task.id)
            }
        }
    }
    
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDescription by remember { mutableStateOf("") }
    var newTaskCategory by remember { mutableStateOf("") }
    var newTaskDueDate by remember { mutableStateOf("") }
    var newTaskTime by remember { mutableStateOf("") }
    var newTaskPriority by remember { mutableIntStateOf(3) }
    var isInputExpanded by remember { mutableStateOf(false) }
    
    var isSearching by remember { mutableStateOf(false) }
    var expandedTaskId by remember { mutableStateOf<Int?>(null) }
    
    var showCompletionToast by remember { mutableStateOf<String?>(null) }
    var showClearCompletedDialog by remember { mutableStateOf(false) }
    var showDeleteIconForCategory by remember { mutableStateOf<String?>(null) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    var showDeleteCategoryConfirmDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var selectedColorHex by remember { mutableStateOf("#E1BEE7") }

    var showCustomDatePicker by remember { mutableStateOf(false) }
    var showCustomTimePicker by remember { mutableStateOf(false) }
    var datePickerCallback by remember { mutableStateOf<((Calendar) -> Unit)?>(null) }
    var timePickerCallback by remember { mutableStateOf<((Int, Int) -> Unit)?>(null) }
    var initialDateForPicker by remember { mutableStateOf(Calendar.getInstance()) }
    var initialHourForPicker by remember { mutableIntStateOf(9) }
    var initialMinuteForPicker by remember { mutableIntStateOf(0) }

    LaunchedEffect(showCompletionToast) {
        if (showCompletionToast != null) {
            delay(2000)
            showCompletionToast = null
        }
    }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    val isTr = appLanguagePreference == "Türkçe"

    if (showCustomDatePicker) {
        PlancaDatePickerDialog(
            initialDate = initialDateForPicker,
            isTr = isTr,
            onDateSelected = { selDate: Calendar -> 
                datePickerCallback?.invoke(selDate)
                showCustomDatePicker = false
            },
            onDismiss = { showCustomDatePicker = false }
        )
    }

    if (showCustomTimePicker) {
        PlancaTimePickerDialog(
            initialHour = initialHourForPicker,
            initialMinute = initialMinuteForPicker,
            isTr = isTr,
            onTimeSelected = { h: Int, m: Int ->
                timePickerCallback?.invoke(h, m)
                showCustomTimePicker = false
            },
            onDismiss = { showCustomTimePicker = false }
        )
    }

    val drawerScrollState = rememberScrollState()
    val locale = if (isTr) Locale.forLanguageTag("tr") else Locale.ENGLISH

    if (showClearCompletedDialog) {
        AlertDialog(
            onDismissRequest = { showClearCompletedDialog = false },
            title = {
                AppText(
                    text = if (isTr) "Tamamlanmış Görevleri Sil" else "Delete Completed Tasks",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                )
            },
            text = {
                val message = if (selectedCategory != null) {
                    val displayName = getCategoryDisplayName(selectedCategory!!, isTr)
                    if (isTr) "Sadece \"$displayName\" kategorisindeki bugünün tamamlanmış görevlerini silmek istediğinizden emin misiniz?"
                    else "Are you sure you want to delete today's completed tasks in the \"$displayName\" category?"
                } else {
                    if (isTr) "Bugünün tüm tamamlanmış görevlerini silmek istediğinizden emin misiniz?"
                    else "Are you sure you want to delete all of today's completed tasks?"
                }
                AppText(
                    text = message,
                    style = TextStyle(fontSize = 14.sp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCompleted()
                        showClearCompletedDialog = false
                    }
                ) {
                    AppText(
                        text = if (isTr) "Evet, Sil" else "Yes, Delete",
                        style = TextStyle(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearCompletedDialog = false }
                ) {
                    AppText(text = if (isTr) "İptal" else "Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showDeleteCategoryConfirmDialog && categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteCategoryConfirmDialog = false },
            title = {
                AppText(
                    text = if (isTr) "Kategoriyi Sil" else "Delete Category",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                )
            },
            text = {
                AppText(
                    text = if (isTr) 
                        "\"${getCategoryDisplayName(categoryToDelete, true)}\" kategorisini silmek istediğinizden emin misiniz? Bu kategoriye ait görevlerin kategorisi kaldırılacaktır." 
                        else "Are you sure you want to delete the \"${getCategoryDisplayName(categoryToDelete, false)}\" category? Tasks in this category will become uncategorized.",
                    style = TextStyle(fontSize = 14.sp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        categoryToDelete?.let { viewModel.deleteCategory(it) }
                        showDeleteCategoryConfirmDialog = false
                        categoryToDelete = null
                    }
                ) {
                    AppText(
                        text = if (isTr) "Evet, Sil" else "Yes, Delete",
                        style = TextStyle(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteCategoryConfirmDialog = false
                        categoryToDelete = null
                    }
                ) {
                    AppText(text = if (isTr) "İptal" else "Cancel")
                }
            }
        )
    }

    if (showColorPickerDialog) {
        var hue by remember { mutableFloatStateOf(0f) }
        var saturation by remember { mutableFloatStateOf(1f) }
        var value by remember { mutableFloatStateOf(1f) }
        
        LaunchedEffect(Unit) {
            val hsv = FloatArray(3)
            try {
                val colorInt = android.graphics.Color.parseColor(selectedColorHex)
                android.graphics.Color.colorToHSV(colorInt, hsv)
                hue = hsv[0]
                saturation = hsv[1]
                value = hsv[2]
            } catch (e: Exception) {
                hue = 0f
                saturation = 1f
                value = 1f
            }
        }
        
        AlertDialog(
            onDismissRequest = { showColorPickerDialog = false },
            title = {
                AppText(
                    text = if (isTr) "Özel Renk Seç" else "Choose Custom Color",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                )
            },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val currentColorInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
                    val r = android.graphics.Color.red(currentColorInt)
                    val g = android.graphics.Color.green(currentColorInt)
                    val b = android.graphics.Color.blue(currentColorInt)
                    val currentHex = String.format("#%02X%02X%02X", r, g, b)
                    val isColorDarkHex = isColorDark(currentHex)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(currentColorInt))
                            .border(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = currentHex,
                            style = TextStyle(
                                color = if (isColorDarkHex) Color.White else Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .pointerInput(hue) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val position = event.changes.first().position
                                        val s = (position.x / size.width).coerceIn(0f, 1f)
                                        val v = (1f - (position.y / size.height)).coerceIn(0f, 1f)
                                        saturation = s
                                        value = v
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val baseColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                            drawRect(color = baseColor)
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.White, Color.Transparent)
                                )
                            )
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black)
                                )
                            )
                            
                            val handleX = saturation * size.width
                            val handleY = (1f - value) * size.height
                            
                            drawCircle(
                                color = Color.White,
                                radius = 7.dp.toPx(),
                                center = Offset(handleX, handleY)
                            )
                            drawCircle(
                                color = Color(currentColorInt),
                                radius = 5.dp.toPx(),
                                center = Offset(handleX, handleY)
                            )
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.5f),
                                radius = 7.dp.toPx(),
                                center = Offset(handleX, handleY),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppText(
                            text = if (isTr) "Renk Tonu (Hue)" else "Hue",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val position = event.changes.first().position
                                            val h = (position.x / size.width).coerceIn(0f, 1f) * 360f
                                            hue = h
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val hueColors = listOf(
                                    Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
                                    Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000)
                                )
                                drawRect(brush = Brush.horizontalGradient(colors = hueColors))
                                
                                val selectorX = (hue / 360f) * size.width
                                drawRoundRect(
                                    color = Color.White,
                                    topLeft = Offset(selectorX - 3.dp.toPx(), -2.dp.toPx()),
                                    size = androidx.compose.ui.geometry.Size(6.dp.toPx(), size.height + 4.dp.toPx()),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalColorInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
                        selectedColorHex = String.format("#%02X%02X%02X", android.graphics.Color.red(finalColorInt), android.graphics.Color.green(finalColorInt), android.graphics.Color.blue(finalColorInt))
                        showColorPickerDialog = false
                    }
                ) {
                    AppText(text = if (isTr) "Seç" else "Select")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showColorPickerDialog = false }
                ) {
                    AppText(text = if (isTr) "İptal" else "Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
                drawerContentColor = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.width(310.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, top = 28.dp, end = 28.dp, bottom = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((-3).dp)
                    ) {
                        PlancaIcon(
                            modifier = Modifier.size(32.dp),
                            primaryColor = MaterialTheme.colorScheme.primary,
                            backgroundColor = MaterialTheme.colorScheme.background
                        )
                        AppText(
                            text = "lanca",
                            style = TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = (-0.5).sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    AppText(
                        text = if (isTr) "Zihninizi serbest bırakın." else "Free your mind.",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (drawerScrollState.value > 0) 0.08f else 0f
                    ),
                    thickness = 0.5.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(drawerScrollState)
                        .padding(start = 28.dp, end = 28.dp, bottom = 28.dp)
                ) {
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    val completedCount = unfilteredTasks.count { it.isCompleted }
                    val activeCount = unfilteredTasks.count { !it.isCompleted }
                    
                    AppText(
                        text = if (isTr) "TOPLAM GÖREV DURUMU" else "TOTAL TASK STATUS",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                        ) {
                            AppText(if (isTr) "Aktif" else "Active", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            AppText(activeCount.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                        ) {
                            AppText(if (isTr) "Tamamlanan" else "Completed", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            AppText(completedCount.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    AppText(
                        text = if (isTr) "KATEGORİLER" else "CATEGORIES",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    NavigationDrawerItem(
                        label = { 
                            AppText(
                                text = if (isTr) "Tüm Kategoriler" else "All Categories",
                                style = TextStyle(fontSize = 13.sp)
                            ) 
                        },
                        selected = selectedCategory == null,
                        onClick = {
                            viewModel.selectCategory(null)
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            unselectedContainerColor = Color.Transparent,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.onBackground
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    var draggedCategoryIndex by remember { mutableStateOf<Int?>(null) }
                    var dragOffsetY by remember { mutableFloatStateOf(0f) }
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val itemHeightPx = with(density) { 50.dp.toPx() }

                    categories.forEachIndexed { index, cat ->
                        val catColorHex = viewModel.getCategoryColor(cat, isDarkTheme)
                        val catColor = Color(android.graphics.Color.parseColor(catColorHex))
                        val isBeingDragged = draggedCategoryIndex == index

                        val targetTranslationY = if (isBeingDragged) {
                            dragOffsetY
                        } else if (draggedCategoryIndex != null) {
                            val offsetInItems = (dragOffsetY / itemHeightPx).roundToInt()
                            val targetIndex = (draggedCategoryIndex!! + offsetInItems).coerceIn(0, categories.size - 1)
                            if (targetIndex > draggedCategoryIndex!!) {
                                if (index in (draggedCategoryIndex!! + 1)..targetIndex) -itemHeightPx else 0f
                            } else if (targetIndex < draggedCategoryIndex!!) {
                                if (index in targetIndex until draggedCategoryIndex!!) itemHeightPx else 0f
                            } else {
                                0f
                            }
                        } else {
                            0f
                        }

                        val animatedTranslationY by animateFloatAsState(
                            targetValue = targetTranslationY,
                            animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
                            label = "category_slide"
                        )

                        val translationY = if (isBeingDragged) dragOffsetY else animatedTranslationY

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    this.translationY = translationY
                                    this.shadowElevation = if (isBeingDragged) 16f else 0f
                                    this.scaleX = if (isBeingDragged) 1.05f else 1.0f
                                    this.scaleY = if (isBeingDragged) 1.05f else 1.0f
                                }
                                .pointerInput(cat) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedCategoryIndex = index
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                        },
                                        onDragEnd = {
                                            val startIdx = draggedCategoryIndex
                                            if (startIdx != null) {
                                                val offsetInItems = Math.round(dragOffsetY / itemHeightPx).toInt()
                                                val targetIndex = (startIdx + offsetInItems).coerceIn(0, categories.size - 1)
                                                if (targetIndex != startIdx) {
                                                    viewModel.moveCategory(startIdx, targetIndex)
                                                }
                                            }
                                            draggedCategoryIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggedCategoryIndex = null
                                            dragOffsetY = 0f
                                        }
                                    )
                                }
                                .padding(bottom = 6.dp)
                        ) {
                            NavigationDrawerItem(
                                label = { 
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AppText(
                                            text = getCategoryDisplayName(cat, isTr),
                                            style = TextStyle(fontSize = 13.sp),
                                            modifier = Modifier.weight(1f)
                                        )
                                        
                                        if (selectedCategory.equals(cat, ignoreCase = true) || showDeleteIconForCategory == cat) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                                    .clickable {
                                                        categoryToDelete = cat
                                                        showDeleteCategoryConfirmDialog = true
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Category",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                icon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(catColor)
                                    )
                                },
                                selected = selectedCategory.equals(cat, ignoreCase = true),
                                onClick = {
                                    viewModel.selectCategory(cat)
                                    showDeleteIconForCategory = cat
                                    scope.launch { drawerState.close() }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = catColor.copy(alpha = 0.15f),
                                    unselectedContainerColor = Color.Transparent,
                                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unselectedTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    var newCategoryName by remember { mutableStateOf("") }
                    val colorPresets = listOf("#E1BEE7", "#BBDEFB", "#C8E6C9", "#FFE0B2", "#F8BBD0", "#B2DFDB", "#FFF9C4")
                    
                    LaunchedEffect(newCategoryName) {
                        if (newCategoryName.isNotBlank()) {
                            delay(150)
                            drawerScrollState.animateScrollTo(drawerScrollState.maxValue)
                        }
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        if (categories.size >= 20) {
                            AppText(
                                text = if (isTr) "Maksimum 20 kategori sınırına ulaşıldı." else "Maximum of 20 categories reached.",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                ),
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppTextField(
                                    value = newCategoryName,
                                    onValueChange = { newCategoryName = it },
                                    singleLine = true,
                                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                scope.launch {
                                                    delay(250)
                                                    drawerScrollState.animateScrollTo(drawerScrollState.maxValue)
                                                }
                                            }
                                        },
                                    decorationBox = { innerTextField ->
                                        if (newCategoryName.isEmpty()) {
                                            AppText(
                                                text = if (isTr) "Yeni kategori..." else "New category...",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                fontSize = 13.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (newCategoryName.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            viewModel.addCustomCategory(newCategoryName, selectedColorHex)
                                            newCategoryName = ""
                                        },
                                        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            
                            if (newCategoryName.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    colorPresets.forEach { colorHex ->
                                        val colorVal = Color(android.graphics.Color.parseColor(colorHex))
                                        val isColorSelected = selectedColorHex == colorHex
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp).clip(RoundedCornerShape(50)).background(colorVal)
                                                .border(width = if (isColorSelected) 1.5.dp else 0.dp, color = if (isColorSelected) MaterialTheme.colorScheme.primary else Color.Transparent, shape = RoundedCornerShape(50))
                                                .clickable { selectedColorHex = colorHex }
                                        )
                                    }
                                    
                                    if (!colorPresets.contains(selectedColorHex)) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp).clip(RoundedCornerShape(50)).background(Color(android.graphics.Color.parseColor(selectedColorHex)))
                                                .border(width = 1.5.dp, color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50))
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp).clip(RoundedCornerShape(50))
                                            .background(if (showColorPickerDialog) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                            .clickable { showColorPickerDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Custom Color", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(if (newCategoryName.isNotBlank()) 180.dp else 32.dp))
                    AppText(
                        text = if (isTr) "Sürüm: ${BuildConfig.VERSION_NAME}" else "Version: ${BuildConfig.VERSION_NAME}",
                        style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.ime))
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.size(44.dp)) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), modifier = Modifier.size(20.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((-2).dp)) {
                            PlancaIcon(modifier = Modifier.size(24.dp))
                            AppText(text = "lanca", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.5).sp, color = MaterialTheme.colorScheme.primary))
                        }
                        IconButton(onClick = onSettingsClick, modifier = Modifier.size(44.dp)) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Box(
                        modifier = Modifier.fillMaxWidth().pointerInput(selectedViewDate) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDrag += dragAmount
                                },
                                onDragEnd = {
                                    if (totalDrag > 100f) {
                                        val newDate = selectedViewDate.clone() as Calendar
                                        newDate.add(Calendar.DAY_OF_YEAR, -1)
                                        viewModel.setSelectedDate(newDate)
                                    } else if (totalDrag < -100f) {
                                        val newDate = selectedViewDate.clone() as Calendar
                                        newDate.add(Calendar.DAY_OF_YEAR, 1)
                                        viewModel.setSelectedDate(newDate)
                                    }
                                }
                            )
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            IconButton(onClick = {
                                val newDate = selectedViewDate.clone() as Calendar
                                newDate.add(Calendar.DAY_OF_YEAR, -1)
                                viewModel.setSelectedDate(newDate)
                            }, modifier = Modifier.size(44.dp)) {
                                Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                            }
                            
                            val dayLabel = remember(selectedViewDate, appLanguagePreference) {
                                val today = Calendar.getInstance()
                                if (today.get(Calendar.YEAR) == selectedViewDate.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == selectedViewDate.get(Calendar.DAY_OF_YEAR)) {
                                    if (isTr) "Bugün" else "Today"
                                } else {
                                    SimpleDateFormat("EEEE", locale).format(selectedViewDate.time).replaceFirstChar { it.uppercase() }
                                }
                            }
                            
                            val dateLabel = remember(selectedViewDate, appLanguagePreference) {
                                val datePart = SimpleDateFormat("d MMMM", locale).format(selectedViewDate.time)
                                val today = Calendar.getInstance()
                                if (today.get(Calendar.YEAR) == selectedViewDate.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == selectedViewDate.get(Calendar.DAY_OF_YEAR)) {
                                    "$datePart, ${SimpleDateFormat("EEEE", locale).format(selectedViewDate.time)}"
                                } else {
                                    datePart
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {
                                    initialDateForPicker = selectedViewDate
                                    datePickerCallback = { viewModel.setSelectedDate(it) }
                                    showCustomDatePicker = true
                                }),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AppText(text = dayLabel, style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.5).sp, color = MaterialTheme.colorScheme.onBackground), textAlign = TextAlign.Center)
                                AppText(text = dateLabel, style = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Normal), textAlign = TextAlign.Center)
                            }
                            
                            IconButton(onClick = {
                                val newDate = selectedViewDate.clone() as Calendar
                                newDate.add(Calendar.DAY_OF_YEAR, 1)
                                viewModel.setSelectedDate(newDate)
                            }, modifier = Modifier.size(44.dp)) {
                                Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Forward", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    
                    selectedCategory?.let { cat ->
                        val catColor = Color(android.graphics.Color.parseColor(viewModel.getCategoryColor(cat, isDarkTheme)))
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(catColor.copy(alpha = 0.12f)).border(1.dp, catColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(catColor))
                                AppText(text = getCategoryDisplayName(cat, isTr), style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground))
                                IconButton(onClick = { viewModel.selectCategory(null) }, modifier = Modifier.size(16.dp)) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear Filter", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (isSearching) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f), RoundedCornerShape(24.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) AppText(text = if (isTr) "Görevlerde ara..." else "Search in tasks...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), style = TextStyle(fontSize = 15.sp))
                                AppTextField(value = searchQuery, onValueChange = { viewModel.setSearchQuery(it) }, singleLine = true, textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth().testTag("search_input"))
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }, modifier = Modifier.size(20.dp)) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    
                    val inputBgColor = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color(0xFFF1F2FA)
                    val inputBorderColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(inputBgColor).border(1.dp, inputBorderColor, RoundedCornerShape(24.dp))
                            .clickable { isInputExpanded = true }.padding(start = 18.dp, top = 14.dp, bottom = 14.dp, end = 18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (newTaskTitle.isEmpty()) AppText(text = if (isTr) "Yeni bir görev yazın..." else "Write a new task...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f), style = TextStyle(fontSize = 15.sp))
                                AppTextField(
                                    value = newTaskTitle, onValueChange = { newTaskTitle = it; if (it.isNotEmpty()) isInputExpanded = true }, singleLine = true,
                                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth().testTag("task_input")
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isInputExpanded = !isInputExpanded }, modifier = Modifier.size(36.dp)) {
                                    Icon(imageVector = if (isInputExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = "Expand", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                if (newTaskTitle.isEmpty()) {
                                    IconButton(onClick = { isSearching = !isSearching; if (!isSearching) viewModel.setSearchQuery("") }, modifier = Modifier.size(36.dp)) {
                                        Icon(imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                        
                        if (isInputExpanded) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.06f), thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))
                                AppText(text = if (isTr) "DETAYLAR / AÇIKLAMA" else "DETAILS / DESCRIPTION", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)))
                                Spacer(modifier = Modifier.height(6.dp))
                                AppTextField(value = newTaskDescription, onValueChange = { newTaskDescription = it }, textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.04f), RoundedCornerShape(8.dp)).padding(8.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                AppText(text = if (isTr) "TARİH VE SAAT" else "DATE AND TIME", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)))
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (newTaskDueDate.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.04f)).border(width = 1.dp, color = if (newTaskDueDate.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Transparent, shape = RoundedCornerShape(12.dp))
                                        .clickable { initialDateForPicker = selectedViewDate.clone() as Calendar; datePickerCallback = { newTaskDueDate = SimpleDateFormat("d MMMM yyyy", locale).format(it.time) }; showCustomDatePicker = true }
                                        .padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center
                                    ) {
                                        AppText(text = if (newTaskDueDate.isBlank()) (if (isTr) "Tarih Seç" else "Set Date") else newTaskDueDate, fontSize = 12.sp, fontWeight = if (newTaskDueDate.isNotBlank()) FontWeight.Medium else FontWeight.Normal, color = if (newTaskDueDate.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground)
                                    }
                                    Box(
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (newTaskTime.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.04f)).border(width = 1.dp, color = if (newTaskTime.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Transparent, shape = RoundedCornerShape(12.dp))
                                        .clickable { initialHourForPicker = 9; initialMinuteForPicker = 0; timePickerCallback = { h, min -> newTaskTime = String.format(Locale.US, "%02d:%02d", h, min) }; showCustomTimePicker = true }
                                        .padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center
                                    ) {
                                        AppText(text = if (newTaskTime.isBlank()) (if (isTr) "Saat Seç" else "Set Time") else newTaskTime, fontSize = 12.sp, fontWeight = if (newTaskTime.isNotBlank()) FontWeight.Medium else FontWeight.Normal, color = if (newTaskTime.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                AppText(text = if (isTr) "ÖNCELİK" else "PRIORITY", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    (1..5).forEach { prio ->
                                        val isSelected = newTaskPriority == prio
                                        Box(
                                            modifier = Modifier.size(36.dp).clip(CircleShape).background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)).border(width = if (isSelected) 2.dp else 1.dp, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), shape = CircleShape).clickable { newTaskPriority = prio }.testTag("priority_selector_$prio"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AppText(text = prio.toString(), style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant))
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                if (selectedCategory == null) {
                                    AppText(text = if (isTr) "KATEGORİ" else "CATEGORY", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        categories.forEach { cat ->
                                            val isCatSelected = newTaskCategory.equals(cat, ignoreCase = true)
                                            val catColor = Color(android.graphics.Color.parseColor(viewModel.getCategoryColor(cat, isDarkTheme)))
                                            Box(
                                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (isCatSelected) catColor.copy(alpha = 0.85f) else catColor.copy(alpha = 0.25f)).border(width = 1.dp, color = if (isCatSelected) catColor else Color.Transparent, shape = RoundedCornerShape(8.dp)).clickable { newTaskCategory = if (isCatSelected) "" else cat }.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                AppText(text = getCategoryDisplayName(cat, isTr), fontSize = 11.sp, fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal, color = if (isDarkTheme) (if (isCatSelected) Color(0xFF111318) else MaterialTheme.colorScheme.onBackground) else MaterialTheme.colorScheme.onBackground)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { isInputExpanded = false; newTaskTitle = ""; newTaskDescription = ""; newTaskCategory = ""; newTaskDueDate = ""; newTaskTime = ""; newTaskPriority = 3; focusManager.clearFocus() }) {
                                        AppText(if (isTr) "İptal" else "Cancel", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                                    }
                                    val formattedDueDate = when {
                                        newTaskDueDate.isNotBlank() && newTaskTime.isNotBlank() -> "$newTaskDueDate, $newTaskTime"
                                        newTaskDueDate.isNotBlank() -> newTaskDueDate
                                        newTaskTime.isNotBlank() -> newTaskTime
                                        else -> ""
                                    }
                                    Button(
                                        onClick = {
                                            if (newTaskTitle.isNotBlank()) {
                                                viewModel.addTask(newTaskTitle, newTaskDescription, selectedCategory ?: newTaskCategory, formattedDueDate, newTaskPriority)
                                                newTaskTitle = ""; newTaskDescription = ""; newTaskCategory = ""; newTaskDueDate = ""; newTaskTime = ""; newTaskPriority = 3; isInputExpanded = false; keyboardController?.hide(); focusManager.clearFocus()
                                            }
                                        },
                                        enabled = newTaskTitle.isNotBlank(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        AppText(if (isTr) "Görev Ekle" else "Add Task", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                        FilterTab(label = if (isTr) "Tümü" else "All", isSelected = currentFilter == TaskFilter.ALL, onClick = { viewModel.setFilter(TaskFilter.ALL) })
                        Spacer(modifier = Modifier.width(16.dp))
                        FilterTab(label = if (isTr) "Aktifler" else "Active", isSelected = currentFilter == TaskFilter.ACTIVE, onClick = { viewModel.setFilter(TaskFilter.ACTIVE) })
                        Spacer(modifier = Modifier.width(16.dp))
                        FilterTab(label = if (isTr) "Tamamlananlar" else "Completed", isSelected = currentFilter == TaskFilter.COMPLETED, onClick = { viewModel.setFilter(TaskFilter.COMPLETED) })
                        Spacer(modifier = Modifier.weight(1f))
                        if (currentFilter == TaskFilter.COMPLETED && tasks.isNotEmpty()) {
                            IconButton(onClick = { showClearCompletedDialog = true }, modifier = Modifier.size(44.dp)) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (tasks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AppText(text = if (searchQuery.isNotEmpty()) (if (isTr) "Sonuç bulunamadı" else "No results found") else (if (isTr) "Yapılacak görev yok." else "No tasks left."), style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.primary))
                                Spacer(modifier = Modifier.height(4.dp))
                                AppText(text = if (searchQuery.isNotEmpty()) (if (isTr) "Farklı bir şey aramayı deneyin." else "Try searching for something else.") else (if (isTr) "Zihniniz serbest, bugününüz tertemiz." else "Your mind is free, your today is blank."), style = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant), textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                            items(items = tasks, key = { it.id }) { task ->
                                TaskRow(
                                    task = task, isExpanded = expandedTaskId == task.id,
                                    onToggleExpand = { expandedTaskId = if (expandedTaskId == task.id) null else task.id },
                                    onToggleComplete = { 
                                        val wasCompleted = task.isCompleted
                                        viewModel.toggleTaskCompletion(task)
                                        if (!wasCompleted) showCompletionToast = if (isTr) "Görev tamamlandı" else "Task completed"
                                    },
                                    onDelete = { viewModel.deleteTask(task); NotificationHelper.cancelTaskReminder(context, task.id) },
                                    onUpdate = { t, desc, prio, cat, due -> viewModel.updateTaskDetails(task, t, desc, cat, due, prio) },
                                    getCategoryColor = { viewModel.getCategoryColor(it, isDarkTheme) },
                                    categories = categories, isTr = isTr, modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
            
            androidx.compose.animation.AnimatedVisibility(
                visible = showCompletionToast != null,
                enter = fadeIn() + androidx.compose.animation.slideInVertically(),
                exit = fadeOut() + androidx.compose.animation.slideOutVertically(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 44.dp)
            ) {
                showCompletionToast?.let { message ->
                    Surface(
                        color = Color(0xFF212124), contentColor = Color.White, shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp, shadowElevation = 8.dp,
                        modifier = Modifier.padding(horizontal = 24.dp).testTag("completion_toast")
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(16.dp))
                            AppText(text = message, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White))
                        }
                    }
                }
            }
        }
    }
}

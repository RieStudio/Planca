package com.planca.app.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.planca.app.PlancaWidgetSmallProvider
import com.planca.app.PlancaWidgetMediumProvider
import com.planca.app.PlancaWidgetLargeProvider
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.planca.app.ui.components.*
import com.planca.app.util.LogHelper
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class WidgetThemeData(
    val id: String,
    val nameTr: String,
    val nameEn: String,
    val background: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val cardBg: Color,
    val isDark: Boolean
)

data class WidgetSizeOption(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

data class MockWidgetTask(
    val title: String,
    val category: String,
    val categoryColor: Color,
    val isCompleted: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSetupScreen(
    currentLanguage: String,
    onBack: () -> Unit
) {
    androidx.activity.compose.BackHandler { onBack() }
    val isTr = currentLanguage == "Türkçe"
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("planca_prefs", Context.MODE_PRIVATE) }

    var selectedSize by remember { mutableStateOf(prefs.getString("widget_size", "orta") ?: "orta") }
    var selectedThemeId by remember { mutableStateOf(prefs.getString("widget_theme", "modern_koyu") ?: "modern_koyu") }
    var opacity by remember { mutableFloatStateOf(prefs.getFloat("widget_opacity", 0.9f)) }
    var filterType by remember { mutableStateOf(prefs.getString("widget_filter_type", "bugun") ?: "bugun") }
    var selectedCategory by remember { mutableStateOf(prefs.getString("widget_filter_category", "İş") ?: "İş") }

    val widgetThemes = remember {
        listOf(
            WidgetThemeData("modern_koyu", "Koyu", "Dark", Color(0xFF0F172A), Color.White, Color(0xFF94A3B8), Color(0xFF38BDF8), Color(0xFF1E293B), true),
            WidgetThemeData("klasik_beyaz", "Klasik Beyaz", "Classic Light", Color(0xFFFFFFFF), Color(0xFF0F172A), Color(0xFF64748B), Color(0xFF2563EB), Color(0xFFF1F5F9), false),
            WidgetThemeData("neon_mavi", "Neon Mavi", "Neon Blue", Color(0xFF020617), Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF00F5FF), Color(0xFF0B1329), true),
            WidgetThemeData("doga_yesili", "Doğa Yeşili", "Nature Green", Color(0xFF064E3B), Color(0xFFECFDF5), Color(0xFFA7F3D0), Color(0xFF10B981), Color(0xFF065F46), true),
            WidgetThemeData("pastel_pembe", "Pastel Pembe", "Pastel Pink", Color(0xFFFFF1F2), Color(0xFF881337), Color(0xFFFB7185), Color(0xFFEC4899), Color(0xFFFFE4E6), false)
        )
    }

    val currentWidgetTheme = widgetThemes.find { it.id == selectedThemeId } ?: widgetThemes[0]

    val categories = remember {
        val saved = prefs.getString("custom_categories_list", null)
        if (saved == null) listOf("İş", "Kişisel", "Sağlık", "Okul", "Alışveriş")
        else try {
            val jsonArray = JSONArray(saved)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) { list.add(jsonArray.getString(i)) }
            list
        } catch (e: Exception) {
            listOf("İş", "Kişisel", "Sağlık", "Okul", "Alışveriş")
        }
    }
    
    val categoryColorsMap = remember {
        val saved = prefs.getString("custom_category_colors_map", null)
        val defaultMap = mapOf(
            "İş" to Color(0xFFBBDEFB),
            "Kişisel" to Color(0xFFE1BEE7),
            "Sağlık" to Color(0xFFC8E6C9),
            "Okul" to Color(0xFFFFE0B2),
            "Alışveriş" to Color(0xFFF8BBD0)
        )
        if (saved == null) defaultMap
        else try {
            val jsonObject = JSONObject(saved)
            val map = mutableMapOf<String, Color>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = Color(android.graphics.Color.parseColor(jsonObject.getString(key)))
            }
            map
        } catch (e: Exception) {
            defaultMap
        }
    }

    val mockTasks = remember {
        listOf(
            MockWidgetTask("Su İç", "Sağlık", Color(0xFFC8E6C9)),
            MockWidgetTask("Spora Git", "Kişisel", Color(0xFFE1BEE7)),
            MockWidgetTask("Kitap Oku", "Hobi", Color(0xFFBBDEFB))
        )
    }

    fun requestPinWidget(context: Context, size: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val providerClass = when (size) {
                "kucuk" -> PlancaWidgetSmallProvider::class.java
                "orta" -> PlancaWidgetMediumProvider::class.java
                else -> PlancaWidgetLargeProvider::class.java
            }
            val myProvider = ComponentName(context, providerClass)

            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                appWidgetManager.requestPinAppWidget(myProvider, null, null)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { AppText(if (isTr) "Widget Ayarları" else "Widget Settings", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())
        ) {
            // Widget Preview Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (MaterialTheme.colorScheme.surface == Color.White) 
                                listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                            else 
                                listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        )
                    )
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                WidgetPreviewFrame(
                    size = selectedSize,
                    themeData = currentWidgetTheme,
                    opacity = opacity,
                    mockTasks = mockTasks,
                    isTr = isTr
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Şeffaflık Ayarı
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        AppText(if (isTr) "ŞEFFAFLIK" else "OPACITY", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                        AppText("%${(opacity * 100).toInt()}", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = opacity,
                        onValueChange = { opacity = it },
                        valueRange = 0.1f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )
                }

                // Tema Seçimi
                Column {
                    AppText(if (isTr) "TEMA SEÇİMİ" else "THEME SELECTION", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        widgetThemes.forEach { theme ->
                            val isSelected = selectedThemeId == theme.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { selectedThemeId = theme.id }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(theme.background)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = theme.textPrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                AppText(if (isTr) theme.nameTr else theme.nameEn, fontSize = 10.sp)
                            }
                        }
                    }
                }

                Column {
                    AppText(if (isTr) "WIDGET BOYUTU" else "WIDGET SIZE", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val sizes = listOf(
                            WidgetSizeOption("kucuk", if (isTr) "Küçük" else "Small", Icons.Default.Star),
                            WidgetSizeOption("orta", if (isTr) "Orta" else "Medium", Icons.Default.Menu),
                            WidgetSizeOption("buyuk", if (isTr) "Büyük" else "Large", Icons.Default.Home)
                        )
                        sizes.forEach { sizeOption ->
                            val isSelected = selectedSize == sizeOption.id
                            Button(
                                onClick = { selectedSize = sizeOption.id },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                elevation = null
                            ) {
                                AppText(sizeOption.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        prefs.edit().apply {
                            putString("widget_size", selectedSize)
                            putString("widget_theme", selectedThemeId)
                            putFloat("widget_opacity", opacity)
                            putString("widget_filter_type", filterType)
                            putString("widget_filter_category", selectedCategory)
                            apply()
                        }
                        
                        // Update widgets
                        val updateIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                        context.sendBroadcast(updateIntent)
                        
                        // Request to pin widget to home screen
                        requestPinWidget(context, selectedSize)
                        
                        Toast.makeText(context, if (isTr) "Ayarlar kaydedildi" else "Settings saved", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    AppText(if (isTr) "Widget'ı Ana Ekrana Ekle" else "Add Widget to Home Screen", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun WidgetPreviewFrame(
    size: String,
    themeData: WidgetThemeData,
    opacity: Float,
    mockTasks: List<MockWidgetTask>,
    isTr: Boolean
) {
    val widgetBg = themeData.background.copy(alpha = opacity)
    val width = when(size) {
        "kucuk" -> 150.dp
        "orta" -> 260.dp
        else -> 300.dp
    }
    val height = when(size) {
        "kucuk" -> 150.dp
        "orta" -> 170.dp
        else -> 240.dp
    }

    Box(
        modifier = Modifier
            .size(width, height)
            .shadow(16.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(widgetBg)
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    AppText("Planca", color = themeData.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    AppText(if (isTr) "Bugün" else "Today", color = themeData.textSecondary, fontSize = 11.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val visibleTasks = if (size == "kucuk") mockTasks.take(2) else if (size == "orta") mockTasks.take(3) else mockTasks
            
            visibleTasks.forEach { task ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(16.dp).border(1.5.dp, themeData.textSecondary.copy(alpha = 0.6f), CircleShape))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        AppText(task.title, color = themeData.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        if (size != "kucuk") {
                            AppText(task.category, color = task.categoryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

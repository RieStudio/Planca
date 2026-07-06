package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.TaskDatabase
import com.example.data.TaskRepository
import com.example.ui.TaskViewModel
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java)
        .allowMainThreadQueries()
        .build()
        
    val repository = TaskRepository(database.taskDao())
    val viewModel = TaskViewModel(repository)
    
    // Add seed tasks for high-fidelity screenshot
    viewModel.addTask("Günün ilk görevi", "Planca'nın şık ve minimalist arayüzünü incele.")
    viewModel.addTask("Tamamlanmış bir detay", "Basitçe dokunarak tamamlayabilirsin.")
    
    composeTestRule.setContent {
      MyApplicationTheme {
        PlancaScreen(
          viewModel = viewModel,
          appLanguagePreference = "Türkçe",
          onSettingsClick = {},
          isDarkTheme = false
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    
    database.close()
  }
}

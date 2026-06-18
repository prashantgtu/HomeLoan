package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.data.ThemeManager
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val context = this
      val themeManager = remember { ThemeManager(context) }
      val themePref by themeManager.themeFlow.collectAsState()
      
      val isDarkTheme = when (themePref) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
      }
      
      MyApplicationTheme(darkTheme = isDarkTheme) {
        AppNavigation(themeManager = themeManager)
      }
    }
  }
}

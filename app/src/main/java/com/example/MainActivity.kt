package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MyApplicationTheme
import com.example.webview.TakaCounterWebView

private const val EMERALD_STATUS_BAR_ARGB = 0xFF059669.toInt()
private val EmeraldStatusBarColor = Color(0xFF059669)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(EMERALD_STATUS_BAR_ARGB),
      navigationBarStyle = SystemBarStyle.auto(
        AndroidColor.TRANSPARENT,
        AndroidColor.TRANSPARENT
      )
    )

    setContent {
      MyApplicationTheme {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
        ) {
          // Paint the edge-to-edge status bar area in #059669 Emerald across all Android API levels.
          Spacer(
            modifier = Modifier
              .fillMaxWidth()
              .windowInsetsTopHeight(WindowInsets.statusBars)
              .background(EmeraldStatusBarColor)
          )

          TakaCounterWebView(
            onExitApp = { finish() },
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .navigationBarsPadding()
              .imePadding()
          )
        }
      }
    }
  }
}

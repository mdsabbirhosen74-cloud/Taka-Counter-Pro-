package com.example.webview

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface

/**
 * Native Android bridge exposed to the bundled Taka Counter Pro WebView.
 * Provides reliable native clipboard copy and haptic vibration without altering index.html.
 */
class TakaCounterBridge(private val context: Context) {

  private val mainHandler = Handler(Looper.getMainLooper())

  @JavascriptInterface
  fun copyToClipboard(text: String) {
    mainHandler.post {
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
      if (clipboard != null) {
        val clip = ClipData.newPlainText(CLIP_LABEL, text)
        clipboard.setPrimaryClip(clip)
      }
    }
  }

  @JavascriptInterface
  fun vibrate(durationMs: Long) {
    val safeDuration = durationMs.coerceIn(MIN_VIBRATION_MS, MAX_VIBRATION_MS)
    mainHandler.post {
      try {
        val vibrator = resolveVibrator() ?: return@post
        if (!vibrator.hasVibrator()) return@post

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator.vibrate(
            VibrationEffect.createOneShot(safeDuration, VibrationEffect.DEFAULT_AMPLITUDE)
          )
        } else {
          @Suppress("DEPRECATION")
          vibrator.vibrate(safeDuration)
        }
      } catch (_: Exception) {
        // Ignore gracefully on devices without vibrator hardware or restricted haptics.
      }
    }
  }

  private fun resolveVibrator(): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      manager?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  }

  companion object {
    const val INTERFACE_NAME = "AndroidBridge"
    private const val CLIP_LABEL = "Taka Counter Pro"
    private const val MIN_VIBRATION_MS = 1L
    private const val MAX_VIBRATION_MS = 500L
  }
}

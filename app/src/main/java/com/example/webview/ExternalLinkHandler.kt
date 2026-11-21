package com.example.webview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.R

/**
 * Routes external links (WhatsApp, Telegram, Email, web links) from WebView
 * to their respective native Android applications while keeping local assets in-app.
 */
object ExternalLinkHandler {

  private const val LOCAL_ASSET_PREFIX = "/android_asset/"
  private val ALLOWED_EXTERNAL_SCHEMES = setOf("http", "https", "whatsapp", "tg", "tel", "sms")

  fun shouldOverrideUrl(context: Context, uri: Uri): Boolean {
    val scheme = uri.scheme?.lowercase() ?: return true

    // Keep the bundled local index.html inside the WebView.
    if (scheme == "file" && uri.path?.startsWith(LOCAL_ASSET_PREFIX) == true) {
      return false
    }

    if (scheme == "mailto") {
      openEmailIntent(context, uri)
      return true
    }

    if (scheme in ALLOWED_EXTERNAL_SCHEMES) {
      openExternalViewIntent(context, uri)
      return true
    }

    // Block any unknown or unsupported schemes from navigating inside WebView.
    return true
  }

  private fun openEmailIntent(context: Context, uri: Uri) {
    val sendToIntent = Intent(Intent.ACTION_SENDTO, uri).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
      context.startActivity(sendToIntent)
    } catch (_: ActivityNotFoundException) {
      openExternalViewIntent(context, uri)
    }
  }

  private fun openExternalViewIntent(context: Context, uri: Uri) {
    val viewIntent = Intent(Intent.ACTION_VIEW, uri).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
      context.startActivity(viewIntent)
    } catch (_: ActivityNotFoundException) {
      Toast.makeText(
        context,
        context.getString(R.string.error_no_app_for_link),
        Toast.LENGTH_SHORT
      ).show()
    }
  }
}

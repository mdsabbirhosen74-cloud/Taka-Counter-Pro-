package com.example.webview

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R

private const val LOCAL_INDEX_URL = "file:///android_asset/index.html"

/**
 * Runtime-only JS hook injected after page load (without modifying index.html on disk)
 * to ensure clipboard copying and haptic vibration work reliably via Android native APIs.
 */
private const val NATIVE_BRIDGE_HOOK_JS = """
  (function() {
    if (window.__androidBridgeHooked || !window.AndroidBridge) return;
    window.__androidBridgeHooked = true;

    var originalCopy = window.copyTextToClipboard;
    window.copyTextToClipboard = function(text) {
      try {
        window.AndroidBridge.copyToClipboard(String(text || ''));
      } catch (e) {}
      if (typeof originalCopy === 'function') {
        try { originalCopy(text); } catch (e) {}
      }
    };

    if (typeof navigator !== 'undefined') {
      try {
        navigator.vibrate = function(pattern) {
          var duration = Array.isArray(pattern) ? Number(pattern[0] || 15) : Number(pattern || 15);
          if (!isNaN(duration) && duration > 0) {
            window.AndroidBridge.vibrate(Math.round(duration));
          }
          return true;
        };
      } catch (e) {}
    }
  })();
"""

/**
 * Checks whether any modal dialog inside index.html is currently open and closes the topmost one.
 */
private const val CLOSE_ACTIVE_MODAL_JS = """
  (function() {
    var modalIds = ['confirmModal', 'saveModal', 'historyModal', 'aboutModal'];
    for (var i = 0; i < modalIds.length; i++) {
      var id = modalIds[i];
      var el = document.getElementById(id);
      if (el && el.classList.contains('active')) {
        if (id === 'confirmModal' && typeof closeConfirmModal === 'function') {
          closeConfirmModal();
        } else if (typeof closeModal === 'function') {
          closeModal(id);
        } else {
          el.classList.remove('active');
        }
        return 'modal_closed';
      }
    }
    return 'none';
  })();
"""

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TakaCounterWebView(
  onExitApp: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val description = stringResource(R.string.webview_content_description)
  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  BackHandler(enabled = true) {
    val currentWebView = webViewRef
    if (currentWebView == null) {
      onExitApp()
      return@BackHandler
    }

    currentWebView.evaluateJavascript(CLOSE_ACTIVE_MODAL_JS) { result ->
      val closedModal = result?.contains("modal_closed") == true
      if (!closedModal) {
        if (currentWebView.canGoBack()) {
          currentWebView.goBack()
        } else {
          onExitApp()
        }
      }
    }
  }

  DisposableEffect(lifecycleOwner, webViewRef) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> webViewRef?.onResume()
        Lifecycle.Event.ON_PAUSE -> webViewRef?.onPause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      webViewRef?.apply {
        stopLoading()
        removeJavascriptInterface(TakaCounterBridge.INTERFACE_NAME)
        destroy()
      }
      webViewRef = null
    }
  }

  AndroidView(
    factory = { ctx ->
      WebView(ctx).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(Color.TRANSPARENT)
        overScrollMode = View.OVER_SCROLL_NEVER
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false

        settings.apply {
          javaScriptEnabled = true
          domStorageEnabled = true
          databaseEnabled = true
          allowFileAccess = true
          allowContentAccess = false
          loadsImagesAutomatically = true
          cacheMode = WebSettings.LOAD_DEFAULT
          mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

          // Disable pinch-to-zoom for clean native mobile UX.
          setSupportZoom(false)
          builtInZoomControls = false
          displayZoomControls = false

          // Keep target="_blank" links routing through shouldOverrideUrlLoading.
          setSupportMultipleWindows(false)
          useWideViewPort = true
          loadWithOverviewMode = true
        }

        addJavascriptInterface(TakaCounterBridge(ctx), TakaCounterBridge.INTERFACE_NAME)

        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
          override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
          ): Boolean {
            val uri = request?.url ?: return false
            return ExternalLinkHandler.shouldOverrideUrl(ctx, uri)
          }

          override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (url?.startsWith("file:///android_asset/") == true) {
              view?.evaluateJavascript(NATIVE_BRIDGE_HOOK_JS, null)
            }
          }
        }

        loadUrl(LOCAL_INDEX_URL)
        webViewRef = this
      }
    },
    modifier = modifier
      .testTag("taka_counter_webview")
      .semantics { contentDescription = description }
  )
}

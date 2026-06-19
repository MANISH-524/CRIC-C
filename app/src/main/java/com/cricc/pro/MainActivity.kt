package com.cricc.pro

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader

/**
 * Single-Activity WebView host for the CRIC-C PRO scoring engine.
 *
 * Key points:
 *  - The web app is served via [WebViewAssetLoader] over a stable
 *    https://appassets.androidx.org/ origin (NOT file://). A stable, secure
 *    origin is what makes `localStorage` reliable across Android versions —
 *    this is what allows an in-progress match to survive the app being killed.
 *  - JavaScript is enabled (the scorer needs it) and DOM storage is enabled
 *    (for localStorage). Everything else is locked down: no file access, no
 *    JavaScript interface bridge, no universal/file-URL access.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).apply {
            // edge-to-edge friendly background while loading
            setBackgroundColor(0xFF000000.toInt())

            settings.apply {
                javaScriptEnabled = true          // scorer logic
                domStorageEnabled = true          // ← enables localStorage persistence
                allowFileAccess = false           // we use the asset loader, not file://
                allowContentAccess = false
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = false
                @Suppress("DEPRECATION")
                allowUniversalAccessFromFileURLs = false
                cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                mediaPlaybackRequiresUserGesture = true
            }

            // No addJavascriptInterface — keeps the JS<->native bridge attack surface at zero.

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

                // keep all navigation inside the app's own asset origin
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val host = request.url.host
                    return host != "appassets.androidx.org"
                }
            }
        }

        setContentView(webView)
        webView.loadUrl("https://appassets.androidx.org/assets/index.html")

        // Back button: navigate WebView history first, else default (exit).
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack()
                else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}

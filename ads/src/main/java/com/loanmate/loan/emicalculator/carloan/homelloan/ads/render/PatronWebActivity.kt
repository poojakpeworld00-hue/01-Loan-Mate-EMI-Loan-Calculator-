package com.loanmate.loan.emicalculator.carloan.homelloan.ads.render

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updatePadding
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.analytics.Stats2

class PatronWebActivity : Activity() {
    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent?.getStringExtra(EXTRA_URL)?.takeIf { it.isNotBlank() }
        if (url == null) {
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val root = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setBackgroundColor(Color.WHITE)
        }

        val web = try { WebView(this) } catch (_: Exception) { finish(); return }.apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setBackgroundColor(Color.WHITE)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    val target = request.url
                    if (target.scheme != "http" && target.scheme != "https") {
                        return runCatching {
                            startActivity(
                                Intent(Intent.ACTION_VIEW, target)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }.isSuccess
                    }
                    return false
                }
            }
        }
        webView = web
        root.addView(web)
        setContentView(root)

        fun kitBarsOf(insets: WindowInsetsCompat) = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        fun putFromRoot() {
            val bars = ViewCompat.getRootWindowInsets(root)?.let(::kitBarsOf) ?: return
            root.updatePadding(top = bars.top, bottom = bars.bottom)
        }
        if (root.isAttachedToWindow) putFromRoot() else root.doOnAttach { putFromRoot() }
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = kitBarsOf(insets)
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)

        web.loadUrl(url)
    }

    @Suppress("DEPRECATION", "MissingSuperCall")
    override fun onBackPressed() {
        // An overlay over the screen, not a screen of its own: no Views.show here.
        Stats2.hotkeyPress("back")
        val web = webView
        if (web != null && web.canGoBack()) web.goBack() else finish()
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_URL = "sponsored_url"

        fun kitIntentFor(context: Context, url: String): Intent =
            Intent(context, PatronWebActivity::class.java).putExtra(EXTRA_URL, url)
    }
}

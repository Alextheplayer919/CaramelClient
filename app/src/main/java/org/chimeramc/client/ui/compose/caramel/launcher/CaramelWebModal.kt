package org.chimeramc.client.ui.compose.caramel.launcher

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.chimeramc.client.R
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.CaramelModal
import org.chimeramc.client.ui.compose.caramel.GlassButton
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.sp_

/**
 * The launcher's own browser: a news post's link opens here, not in the system browser.
 *
 * <p>A WebView rather than an intent, so the user never leaves the app. The page is a real one — the
 * same one the link names — and navigation inside it stays in the view. It is the only place the
 * launcher shows a page it did not author, so JavaScript is on (the target is a static site) and
 * nothing else is.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CaramelWebModal(state: CaramelLauncherState) {
    CaramelModal(state.webUrl.isNotEmpty(), { state.webUrl = "" }, Modifier.size(960.dp, 720.dp)) {
        var loading by remember { mutableStateOf(true) }
        var progress by remember { mutableStateOf(0) }
        var loaded by remember { mutableStateOf<WebView?>(null) }
        val title = state.webTitle.ifEmpty { state.webUrl }
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = T.Subtitle)
                    Text(state.webUrl, style = T.Caption, maxLines = 1)
                }
                GlassButton(null, { loaded?.reload() }, icon = Icons.Rounded.Refresh, height = 32)
                GlassButton(null, { state.webUrl = "" }, icon = Icons.Rounded.Close, height = 32)
            }
            if (loading) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = C.Caramel,
                )
            }
            AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        settings.javaScriptEnabled = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                // Keep every hop inside this view; a link inside the page must not
                                // hand the user to the system browser.
                                view.loadUrl(request.url.toString())
                                return true
                            }

                            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                loading = true
                                progress = 0
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                loading = false
                            }
                        }
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                progress = newProgress
                                loading = newProgress < 100
                            }
                        }
                        loaded = this
                    }
                },
                update = { view ->
                    val url = state.webUrl
                    if (url.isNotEmpty() && view.url != url) view.loadUrl(url)
                },
            )
        }
    }
}

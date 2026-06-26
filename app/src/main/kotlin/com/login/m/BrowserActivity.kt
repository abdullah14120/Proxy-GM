package com.login.m

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import java.util.concurrent.Executor

class BrowserActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false 
            databaseEnabled = false
            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE 
        }

        webView.webViewClient = WebViewClient()

        applyDynamicProxy()

        webView.loadUrl("https://login.live.com")
    }

    private fun applyDynamicProxy() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            val currentProxy = ProxyManager.getRandomProxy()
            
            val proxyConfig = ProxyConfig.Builder()
                .addProxyRule("${currentProxy.host}:${currentProxy.port}")
                .addDirect() 
                .build()

            ProxyController.getInstance().setProxyOverride(proxyConfig, Executor { command -> 
                command.run() 
            }, Runnable {
                // Proxy applied successfully
            })
        }
    }

    override fun onDestroy() {
        SessionCleaner.clearAllData(applicationContext)
        webView.destroy()
        super.onDestroy()
    }
}

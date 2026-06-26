package com.engine.core.logic

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import java.util.concurrent.Executor

class GoogleEnvironmentManager(
    private val context: Context, 
    private val webView: WebView
) {

    companion object {
        const val GOOGLE_SIGNUP_URL = "https://accounts.google.com/SignUp"
        const val HIGH_REPUTATION_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro Build/UQ1A.231205.015) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
    }

    init {
        configureWebSettings()
        configureEnvironmentIsolation()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebSettings() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = HIGH_REPUTATION_USER_AGENT
            
            allowFileAccess = false
            allowContentAccess = false
        }
    }

    private fun configureEnvironmentIsolation() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        }
    }

    /**
     * يقوم بتطبيق البروكسي على الـ WebView باستخدام AndroidX Webkit
     * @param host عنوان السيرفر (مثل 127.0.0.1 أو الآي بي الأمريكي)
     * @param port المنفذ الخاص بالبروكسب
     */
    fun applyProxy(host: String, port: Int) {
        // التحقق مما إذا كانت ميزة البروكسي مدعومة من نظام تشغيل الجهاز الحالي
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            
            // إعداد البروكسي (يدعم صيغ مثل http://host:port)
            val proxyUrl = "http://$host:$port"
            
            val proxyConfig = ProxyConfig.Builder()
                .addProxyRule(proxyUrl) // توجيه كافة حركات المرور عبر هذا العنوان
                .addDirect() // في حال فشل البروكسي، يمكن وضع قواعد بديلة أو تركه معلقاً لأمان الجلسة
                .build()

            try {
                ProxyController.getInstance().setProxyOverride(
                    proxyConfig, 
                    { runnable -> runnable.run() }, // Executor لتنفيذ العملية في الخلفية
                    { /* كود اختياري ينفذ عند نجاح أو تغير إعداد البروكسي */ }
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadRegistrationPage() {
        webView.loadUrl(GOOGLE_SIGNUP_URL)
    }
    
    fun clearSessionData() {
        CookieManager.getInstance().removeAllCookies(null)
        webView.clearCache(true)
        
        // إعادة تعيين البروكسي عند إغلاق الجلسة لحماية خصوصية النظام
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            ProxyController.getInstance().clearProxyOverride({ it.run() }, {})
        }
    }
}

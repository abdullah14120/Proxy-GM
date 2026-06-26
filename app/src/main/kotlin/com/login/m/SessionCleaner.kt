package com.login.m

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView

object SessionCleaner {

    fun clearAllData(context: Context) {
        // 1. مسح الكوكيز بالكامل
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()

        // 2. مسح بيانات التخزين المحلي وقواعد البيانات والـ Cache
        WebStorage.getInstance().deleteAllData()

        // 3. مسح كاش الـ WebView على القرص الصلب
        val webView = WebView(context)
        webView.clearCache(true)
        webView.clearFormData()
        webView.clearHistory()
    }
}

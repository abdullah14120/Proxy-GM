package com.engine.core.logic

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.Uri
import android.telephony.SmsManager
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class GoogleEnvironmentManager(
    private val context: Context, 
    private val webView: WebView
) {

    companion object {
        const val GOOGLE_SIGNUP_URL = "https://accounts.google.com/SignUp"
        const val HIGH_REPUTATION_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro Build/UQ1A.231205.015) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
        const val SMS_SENT_ACTION = "com.engine.SMS_SENT"
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
                val url = request?.url?.toString() ?: return false

                // اعتراض طلب إرسال الـ SMS
                if (url.startsWith("sms:")) {
                    parseAndSendSms(url)
                    return true // إرجاع true لمنع المتصفح من إظهار صفحة الخطأ البيضاء
                }
                return false
            }
        }
    }

    /**
     * تفكيك رابط الـ SMS واستخراج الرقم والنص ثم إرساله
     */
    private fun parseAndSendSms(url: String) {
        try {
            // فك تشفير الرابط للتخلص من رموز الـ %D8 وغيرها وتحويلها لنصوص مفهومة
            val decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.name())
            
            // استخراج رقم الهاتف (ما بين sms: وعلامة الاستفهام)
            val uri = Uri.parse(decodedUrl)
            val phoneNumber = uri.schemeSpecificPart.split("?")[0].replace("//", "")
            
            // استخراج نص الرسالة المرفق بالرابط (إن وجد)
            var messageBody = ""
            if (decodedUrl.contains("?body=")) {
                messageBody = decodedUrl.substringAfter("?body=")
            } else if (decodedUrl.contains("&body=")) {
                messageBody = decodedUrl.substringAfter("&body=")
            }

            // تنفيذ الإرسال المؤكد
            executeDirectSms(phoneNumber, messageBody)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطأ في معالجة بيانات الرسالة", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * إرسال الرسالة في الخلفية برمجياً وتتبع النتيجة قطعياً
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun executeDirectSms(phoneNumber: String, messageText: String) {
        val sentPI = PendingIntent.getBroadcast(
            context, 0, Intent(SMS_SENT_ACTION), PendingIntent.FLAG_IMMUTABLE
        )

        // تسجيل مستمع لمعرفة النتيجة من شبكة الاتصال
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(arg0: Context?, arg1: Intent?) {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Toast.makeText(context, "🟢 تم إرسال رسالة التأكيد بنجاح من الشريحة", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Toast.makeText(context, "❌ فشل إرسال الرسالة، تحقق من الرصيد أو التغطية", Toast.LENGTH_LONG).show()
                    }
                }
                context.unregisterReceiver(this) // حماية الذاكرة وإلغاء الاستماع
            }
        }, IntentFilter(SMS_SENT_ACTION))

        // إرسال الرسالة الفعلي عبر النظام
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phoneNumber, null, messageText, sentPI, null)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "حدث خطأ أثناء الإرسال البرمجي", Toast.LENGTH_SHORT).show()
        }
    }

    fun applyProxy(host: String, port: Int) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            val proxyUrl = "http://$host:$port"
            val proxyConfig = ProxyConfig.Builder().addProxyRule(proxyUrl).addDirect().build()
            try {
                ProxyController.getInstance().setProxyOverride(proxyConfig, { it.run() }, {})
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
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            ProxyController.getInstance().clearProxyOverride({ it.run() }, {})
        }
    }
}

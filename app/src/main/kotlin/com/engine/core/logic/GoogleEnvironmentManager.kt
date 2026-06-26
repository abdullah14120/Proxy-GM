package com.engine.core.logic

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.telephony.SmsManager
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class GoogleEnvironmentManager(
    private val context: Context, 
    private val webView: WebView
) {

    companion object {
        // رابط إنشاء الحساب الرسمي القياسي
        const val GOOGLE_SIGNUP_URL = "https://accounts.google.com/SignUp"
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
            cacheMode = WebSettings.LOAD_NO_CACHE // منع الكاش لضمان جلسة نظيفة في كل مرة
            
            // استخدام الـ User-Agent الافتراضي الحقيقي للجهاز الحالي 
            // هذا يضمن تطابق بصمة المتصفح مع بصمة النظام 100% لقوقل
            userAgentString = null 
            
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

                // التقاط رابط الرسالة وتمريره للمحرك الخلفي فوراً
                if (url.startsWith("sms:")) {
                    parseAndSendSms(url)
                    return true 
                }
                return false
            }
        }
    }

    private fun parseAndSendSms(url: String) {
        try {
            val decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.name())
            val uri = Uri.parse(decodedUrl)
            
            // استخراج الرقم والنص بدقة
            val phoneNumber = uri.schemeSpecificPart.split("?")[0].replace("//", "")
            var messageBody = ""
            if (decodedUrl.contains("?body=")) {
                messageBody = decodedUrl.substringAfter("?body=")
            } else if (decodedUrl.contains("&body=")) {
                messageBody = decodedUrl.substringAfter("&body=")
            }

            executeDirectSms(phoneNumber, messageBody)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطأ في قراءة بيانات الرسالة", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun executeDirectSms(phoneNumber: String, messageText: String) {
        val sentPI = PendingIntent.getBroadcast(
            context, 0, Intent(SMS_SENT_ACTION), PendingIntent.FLAG_IMMUTABLE
        )

        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(arg0: Context?, arg1: Intent?) {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Toast.makeText(context, "🟢 تم الإرسال بنجاح! انتظر ثواني ليتحدث الفحص تلقائياً", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Toast.makeText(context, "❌ فشل الإرسال الخلوي، تأكد من وجود رصيد كافٍ في الشريحة", Toast.LENGTH_LONG).show()
                    }
                }
                context.unregisterReceiver(this)
            }
        }, IntentFilter(SMS_SENT_ACTION))

        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phoneNumber, null, messageText, sentPI, null)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل المحرك في إرسال الرسالة", Toast.LENGTH_SHORT).show()
        }
    }

    fun loadRegistrationPage() {
        webView.loadUrl(GOOGLE_SIGNUP_URL)
    }
    
    fun clearSessionData() {
        CookieManager.getInstance().removeAllCookies(null)
        webView.clearCache(true)
    }
}

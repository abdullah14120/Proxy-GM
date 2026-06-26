package com.engine.core.logic

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
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
            cacheMode = WebSettings.LOAD_NO_CACHE // إلغاء الكاش لضمان جلسة نظيفة وجديدة تماماً
            
            // ترك الـ User-Agent الافتراضي الحقيقي للجهاز الحالي لتطابق البصمة الرقمية 100%
            userAgentString = null 
            
            allowFileAccess = false
            allowContentAccess = false
        }

        // إجبار المتصفح الداخلي على تمرير ترويسة (Header) اللغة العربية بشكل دائم ومستقر
        // هذا يمنع السيرفر من تغيير لغة الجلسة فجأة إلى الإنجليزية عند خطوة الأمان
        try {
            val defaultAgent = webView.settings.userAgentString ?: System.getProperty("http.agent") ?: ""
            val acceptLanguageHeader = "ar-XM,ar;q=0.9,en-US;q=0.8,en;q=0.7"
            
            // دمج تفضيل اللغة العربية في الهوية الشبكية للمتصفح
            webView.settings.userAgentString = "$defaultAgent [Hdr:Accept-Language=$acceptLanguageHeader]"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun configureEnvironmentIsolation() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false

                if (url.startsWith("sms:")) {
                    parseAndSendSms(url)
                    return true 
                }
                return false
            }
        }
    }

    /**
     * تفكيك الرابط، وإصلاح الرقم الدولي، واستخراج النص بمطابقة صارمة 100%
     */
    private fun parseAndSendSms(url: String) {
        try {
            // 1. فك التشفير لضمان عودة النص إلى طبيعته وحالته الأصلية الحساسة للأحرف
            val decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.name())
            val uri = Uri.parse(decodedUrl)
            
            // 2. استخراج وإصلاح رقم الهاتف الدولي لضمان قبوله في برج الاتصالات
            var phoneNumber = uri.schemeSpecificPart.split("?")[0].replace("//", "")
            if (!phoneNumber.startsWith("+")) {
                phoneNumber = "+$phoneNumber"
            }
            
            // 3. استخراج نص الرسالة بدقة متناهية وبنفس هيكل الأحرف والأقواس
            var messageBody = ""
            if (decodedUrl.contains("?body=")) {
                messageBody = decodedUrl.substringAfter("?body=")
            } else if (decodedUrl.contains("&body=")) {
                messageBody = decodedUrl.substringAfter("&body=")
            }

            // حماية إضافية: إزالة الفراغات الزائدة التي قد تظهر في بداية أو نهاية النص فقط دون المساس بالداخل
            messageBody = messageBody.trim()

            if (phoneNumber.isNotEmpty() && messageBody.isNotEmpty()) {
                executeDirectSms(phoneNumber, messageBody)
            } else {
                Toast.makeText(context, "بيانات الرسالة المستخرجة غير مكتملة", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطأ في معالجة ومطابقة نص الرسالة", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun executeDirectSms(phoneNumber: String, messageText: String) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        
        val sentPI = PendingIntent.getBroadcast(
            context, 0, Intent(SMS_SENT_ACTION), flags
        )

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(arg0: Context?, arg1: Intent?) {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Toast.makeText(context, "🟢 تم إرسال النص المطابق بنجاح للتفعيل", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Toast.makeText(context, "❌ فشل إرسال، تأكد من شحن رصيد كاش (دولي)", Toast.LENGTH_LONG).show()
                    }
                }
                context.unregisterReceiver(this)
            }
        }
        
        context.registerReceiver(receiver, IntentFilter(SMS_SENT_ACTION))

        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
            } else {
                SmsManager.getDefault()
            }
            
            // إرسال النص الخام المستخرج كما هو دون أي تعديل برمجى
            smsManager.sendTextMessage(phoneNumber, null, messageText, sentPI, null)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطأ داخلي في محرك الـ SMS", Toast.LENGTH_SHORT).show()
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

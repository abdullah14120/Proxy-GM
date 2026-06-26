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
        // رابط إنشاء حساب Google الرسمي القياسي
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
            cacheMode = WebSettings.LOAD_NO_CACHE // إلغاء الكاش لضمان بدء جلسة نظيفة وجديدة بالكامل
            
            // ترك الـ User-Agent افتراضياً ليطابق بصمة جهازك الحقيقي 100% لتفادي الحظر الأمني لقوقل
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

                // اعتراض طلب إرسال الـ SMS لتوليه برمجياً عبر الـ SmsManager
                if (url.startsWith("sms:")) {
                    parseAndSendSms(url)
                    return true // إرجاع true لمنع المتصفح من محاولة تحميل الرابط وإظهار صفحة خطأ بيضاء
                }
                return false
            }
        }
    }

    /**
     * تفكيك رابط الـ SMS واستخراج الرقم الدولي والنص المكتوب
     */
    private fun parseAndSendSms(url: String) {
        try {
            // فك تشفير الرابط (URL Decode) لترميز الكلمات العربية والرموز الخاصة بقوقل مثل %D8
            val decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.name())
            val uri = Uri.parse(decodedUrl)
            
            // عزل رقم الهاتف المستهدف عن بقية المعاملات الأخرى
            val phoneNumber = uri.schemeSpecificPart.split("?")[0].replace("//", "")
            
            // استخراج نص الرسالة المطلوب إرساله للسيرفر
            var messageBody = ""
            if (decodedUrl.contains("?body=")) {
                messageBody = decodedUrl.substringAfter("?body=")
            } else if (decodedUrl.contains("&body=")) {
                messageBody = decodedUrl.substringAfter("&body=")
            }

            // الانتقال إلى دالة الإرسال الخلوية المدعومة
            executeDirectSms(phoneNumber, messageBody)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطأ في قراءة وفك ترميز بيانات الرسالة", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * إرسال الرسالة النصية برمجياً في الخلفية ورصد استجابة برج الاتصالات
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun executeDirectSms(phoneNumber: String, messageText: String) {
        // إشارة تنبيه فريدة يتم إرسالها للـ Receiver عند حسم عملية الإرسال من الشبكة
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        
        val sentPI = PendingIntent.getBroadcast(
            context, 0, Intent(SMS_SENT_ACTION), flags
        )

        // تسجيل مفسر البث المباشر لمعرفة تقرير التسليم الخلوي النهائي
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(arg0: Context?, arg1: Intent?) {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Toast.makeText(context, "🟢 تم الإرسال بنجاح! انتظر ثواني ليتحدث الفحص تلقائياً", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Toast.makeText(context, "❌ فشل الإرسال الخلوي، تأكد من وجود رصيد نقدي كافي للرسائل الدولية", Toast.LENGTH_LONG).show()
                    }
                }
                // إلغاء التسجيل فوراً عند انتهاء المهمة حماية للذاكرة العشوائية من التسريب
                context.unregisterReceiver(this)
            }
        }
        
        context.registerReceiver(receiver, IntentFilter(SMS_SENT_ACTION))

        // محاولة استخدام مدير النظام المناسب لإرسال الرسالة تلقائياً
        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
            } else {
                SmsManager.getDefault()
            }
            
            // تمرير البيانات للمحرك الخلوي للهاتف
            smsManager.sendTextMessage(phoneNumber, null, messageText, sentPI, null)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل محرك النظام في معالجة الإرسال", Toast.LENGTH_SHORT).show()
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

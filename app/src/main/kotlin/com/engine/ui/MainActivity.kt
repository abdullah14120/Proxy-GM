package com.engine.ui

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.engine.core.logic.GoogleEnvironmentManager
import com.engine.R

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var environmentManager: GoogleEnvironmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initCoreLogic()
        
        // إعداد البروكسي الأمريكي (استبدل القيم بالبيانات الفعلية الخاصة بسيرفرك)
        // ملاحظة: الـ ProxyController القياسي يدعم بروتوكول HTTP/HTTPS بشكل افتراضي ومستقر
        environmentManager.applyProxy("us-proxy-address.com", 8080)
        
        // تحميل الصفحة بعد التأكد من ربط البروكسي
        environmentManager.loadRegistrationPage()
    }

    private fun initViews() {
        webView = findViewById(R.id.mainWebView)
    }

    private fun initCoreLogic() {
        // تمرير الـ Context والـ WebView إلى مدير البيئة
        environmentManager = GoogleEnvironmentManager(this, webView)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        environmentManager.clearSessionData()
        super.onDestroy()
    }
}

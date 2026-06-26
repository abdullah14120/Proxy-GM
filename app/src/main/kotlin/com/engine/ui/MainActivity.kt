package com.engine.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.engine.core.logic.GoogleEnvironmentManager
import com.engine.R

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var environmentManager: GoogleEnvironmentManager
    
    companion object {
        private const val SMS_PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initCoreLogic()
        
        // التحقق من الصلاحيات قبل تشغيل المتصفح
        checkSmsPermissionAndStart()
    }

    private fun initViews() {
        webView = findViewById(R.id.mainWebView)
    }

    private fun initCoreLogic() {
        environmentManager = GoogleEnvironmentManager(this, webView)
        environmentManager.applyProxy("us-proxy-address.com", 8080)
    }

    private fun checkSmsPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            // إذا كانت الصلاحية غير ممنوحة، نطلبها رسمياً من العميل عبر واجهة النظام
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_CODE)
        } else {
            // الصلاحية موجودة مسبقاً، نبدأ العمل مباشرة
            environmentManager.loadRegistrationPage()
        }
    }

    // الاستماع لقرار المستخدم بعد ظهور نافذة طلب الصلاحية
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // وافق المستخدم على منح الصلاحية لتطبيقنا
                environmentManager.loadRegistrationPage()
            } else {
                // رفض المستخدم
                Toast.makeText(this, "يجب الموافقة على صلاحية الـ SMS لضمان تأكيد الأرقام تلقائياً", Toast.LENGTH_LONG).show()
                // يمكن أيضاً إعادة إغلاق التطبيق أو تحميل الصفحة مع إشعار المستخدم بالخلل المترتب
                environmentManager.loadRegistrationPage()
            }
        }
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

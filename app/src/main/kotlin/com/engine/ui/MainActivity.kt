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
        checkSmsPermissionAndStart()
    }

    private fun initViews() {
        webView = findViewById(R.id.mainWebView)
    }

    private fun initCoreLogic() {
        environmentManager = GoogleEnvironmentManager(this, webView)
    }

    private fun checkSmsPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_CODE)
        } else {
            environmentManager.loadRegistrationPage()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                environmentManager.loadRegistrationPage()
            } else {
                Toast.makeText(this, "يجب قبول الصلاحية لإرسال الرسالة تلقائياً", Toast.LENGTH_LONG).show()
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

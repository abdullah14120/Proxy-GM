package com.login.m

import android.app.Application
import android.os.Build
import android.webkit.WebView

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // تخصيص دليل بيانات فرعي لكل عملية لمنع الانهيار
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName()
            if (packageName != processName) {
                // إذا كانت هذه هي العملية المنفصلة للمتصفح، نعطيها مجلد تخزين خاص بها
                val suffix = processName.substringAfter(":")
                WebView.setDataDirectorySuffix(suffix)
            }
        }
    }
}

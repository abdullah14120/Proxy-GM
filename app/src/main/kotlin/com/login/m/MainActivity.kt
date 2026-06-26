package com.login.m

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val button = Button(this).apply {
            text = "فتح جلسة متصفح جديدة ونظيفة"
            setOnClickListener {
                // تنظيف الكوكيز والجلسات السابقة
                SessionCleaner.clearAllData(applicationContext)
                
                // الاستدعاء المباشر للكلاس المتواجد في نفس الحزمة
                val intent = Intent(this@MainActivity, BrowserActivity::class.java)
                startActivity(intent)
            }
        }
        
        setContentView(button)
    }
}

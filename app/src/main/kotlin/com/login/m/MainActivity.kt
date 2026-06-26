package com.login.m

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // بناء الواجهة برمجياً بشكل مباشر لتجنب تعقيد ملفات XML الـ Layout
        val button = Button(this).apply {
            text = "فتح جلسة متصفح جديدة ونظيفة"
            setOnClickListener {
                // تصفير أي كوكيز عالقة قبل بدء الجلسة الجديدة
                SessionCleaner.clearAllData(applicationContext)
                
                // الانتقال إلى متصفح الجلسة
                val intent = Intent(this@MainActivity, BrowserActivity::class.java)
                startActivity(intent)
            }
        }
        
        setContentView(button)
    }
}

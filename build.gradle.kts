// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    // موديول بناء تطبيقات أندرويد
    id("com.android.application") version "8.4.0" apply false
    id("com.android.library") version "8.4.0" apply false

    // دعم لغة Kotlin داخل بيئة أندرويد
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

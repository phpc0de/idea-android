buildscript {
    repositories {
        jcenter()
        google()
    }
    dependencies {
        val nav_version = "2.2.0"
        classpath("com.android.tools.build:gradle:3.4.0")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:${nav_version}")
    }
}

allprojects {
    repositories {
        jcenter()
    }
}

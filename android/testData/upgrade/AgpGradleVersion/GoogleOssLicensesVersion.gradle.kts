buildscript {
    repositories {
        jcenter()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.5.0")
        classpath("com.google.android.gms:oss-licenses-plugin:0.10.1")
    }
}

allprojects {
    repositories {
        jcenter()
    }
}

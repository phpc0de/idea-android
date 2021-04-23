buildscript {
    repositories {
        jcenter()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.5.0")
    }
}

plugins {
  id("com.google.android.gms.oss-licenses-plugin") version "0.10.4" apply false
}

allprojects {
    repositories {
        jcenter()
    }
}

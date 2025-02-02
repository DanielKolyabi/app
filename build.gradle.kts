// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.google.devtools.ksp") version "1.8.21-1.0.11" apply false
}
buildscript {
    val agp_version by extra("8.1.2")
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:$agp_version")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Config.Versions.kotlin}")
        classpath("com.google.gms:google-services:4.3.15")
        classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.2")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven("https://jitpack.io")
    }
}

tasks.create("clean", Delete::class) {
    delete(rootProject.buildDir)
}

//task clean(type: Delete) {
//    delete rootProject.buildDir
//}

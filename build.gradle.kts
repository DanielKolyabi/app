// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        jcenter()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Config.Versions.kotlin}")
        classpath("com.google.gms:google-services:4.3.15")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.4")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        maven("https://jitpack.io")
        configurations.all {
            resolutionStrategy {
                force("com.google.android.gms:play-services-location:21.0.1")
            }
        }
    }
}

tasks.create("clean", Delete::class) {
    delete(rootProject.buildDir)
}

//task clean(type: Delete) {
//    delete rootProject.buildDir
//}

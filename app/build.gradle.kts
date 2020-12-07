import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id("com.google.firebase.crashlytics")
    id("com.google.gms.google-services")
}

android {
    signingConfigs {
        create("config") {
            keyAlias = "kurjer"
            keyPassword = "1qazxsw2"
            storeFile = file("2017abix.dat")
            storePassword = "1qazxsw2"
        }
    }
    compileSdkVersion(29)
    defaultConfig {
        applicationId = "abix.kurjer"
        minSdkVersion(21)
        targetSdkVersion(29)
        versionCode = 125
        versionName = "125"
    }
    flavorDimensions("server")
    productFlavors {
        create("productServer") {
            buildConfigField("String", "API_URL", "\"https://courrmobileapi.courdm.ru\"")
        }
        create("debugServer") {
            buildConfigField("String", "API_URL", "\"http://warp.courdm.ru:8084\"")
        }
        create("localServer") {
            buildConfigField("String", "API_URL", "\"http://192.168.31.18:8090\"")
        }
    }
    buildTypes {
        getByName("debug") {
            buildConfigField("String", "YA_KEY", "\"9be78b32-1265-4157-bf15-999b8d747a5c\"")
            signingConfig = signingConfigs.getByName("config")
        }
        getByName("release") {
            buildConfigField("String", "YA_KEY", "\"9be78b32-1265-4157-bf15-999b8d747a5c\"")
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("config")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks {
    withType(KotlinCompile::class) {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs += listOf("-XXLanguage:+NewInference", "-XXLanguage:+InlineClasses", "-Xopt-in=org.mylibrary.OptInAnnotation")
        }
    }
}

androidExtensions {
    isExperimental = true
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to "*.jar")))
    implementation("com.google.android.material:material:1.2.0-alpha02")

    debugImplementation("com.amitshekhar.android:debug-db:1.0.4")

    // Kotlin
    implementation(kotlin("stdlib-jdk7", Config.Versions.kotlin))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Config.Versions.kotlinx}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Config.Versions.kotlinx}")

    // Koin
    implementation("org.koin:koin-android:${Config.Versions.koin}")
    implementation("org.koin:koin-android-ext:${Config.Versions.koin}")

    // androidx
    implementation("androidx.core:core-ktx:1.3.0")
    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.0-beta4")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("androidx.annotation:annotation:1.1.0")

    //Room
    implementation("androidx.room:room-runtime:${Config.Versions.roomVersion}")
    kapt("androidx.room:room-compiler:${Config.Versions.roomVersion}")
    implementation("androidx.room:room-ktx:${Config.Versions.roomVersion}")

    // Glide
    implementation("com.github.bumptech.glide:glide:${Config.Versions.glide}")
    implementation("com.github.bumptech.glide:okhttp3-integration:${Config.Versions.glide}")
    kapt("com.github.bumptech.glide:compiler:${Config.Versions.glide}")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.1.0")

    // Cicerone
    implementation("ru.terrakok.cicerone:cicerone:5.1.1")

    // Firebase
    implementation("com.google.firebase:firebase-messaging:20.2.0")
    implementation("com.google.firebase:firebase-crashlytics:17.2.1")

    //Various
    implementation("com.yandex.android:mapkit:3.3.1")
    implementation("com.google.android.gms:play-services-location:17.0.0")
    implementation("com.github.instacart.truetime-android:library:3.4")
    implementation("joda-time:joda-time:2.10.1")
    implementation("com.mikepenz:materialdrawer:7.0.0-rc08")
}
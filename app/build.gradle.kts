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
            isV1SigningEnabled = true
            isV2SigningEnabled = true
        }
    }
    compileSdk = 33
    defaultConfig {
        applicationId = "abix.kurjer"
        minSdk = 21
        targetSdk = 33
        versionCode = 209
        versionName = "209"
        buildConfigField("Boolean", "FEATURE_PHOTO_RADIUS", "true")
    }
    flavorDimensions += listOf("server")
    productFlavors {
        create("productServer") {
            buildConfigField("String", "API_URL", "\"https://courrmobileapi.courdm.ru\"")
        }
        create("debugServer") {
            buildConfigField("String", "API_URL", "\"http://warp.courdm.ru:8084\"")
        }
        create("debugV2Server") {
            buildConfigField("String", "API_URL", "\"http://warp.courdm.ru:8085\"")
        }
        create("localServer") {
            buildConfigField("String", "API_URL", "\"http://192.168.1.106:8090\"")
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

    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    namespace = "ru.relabs.kurjer"
    packagingOptions {
        resources.excludes.add("META-INF/versions/9/previous-compilation-data.bin")
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
    implementation("com.google.android.material:material:1.8.0")

    debugImplementation("com.amitshekhar.android:debug-db:1.0.4")

    // Kotlin
    implementation(kotlin("stdlib-jdk7", Config.Versions.kotlin))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Config.Versions.kotlinx}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Config.Versions.kotlinx}")

    // Koin
    implementation("io.insert-koin:koin-android:${Config.Versions.koin}")

    // androidx
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0-alpha09")
    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation("androidx.annotation:annotation:1.6.0")

    //Room
    implementation("androidx.room:room-runtime:2.5.1")
    kapt("androidx.room:room-compiler:2.5.1")
    implementation("androidx.room:room-ktx:2.5.1")

    // Glide
    implementation("com.github.bumptech.glide:glide:${Config.Versions.glide}")
    implementation("com.github.bumptech.glide:okhttp3-integration:${Config.Versions.glide}")
    kapt("com.github.bumptech.glide:compiler:${Config.Versions.glide}")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")

    // Cicerone
    implementation("com.github.terrakok:cicerone:7.1")

    // Firebase
    implementation("com.google.firebase:firebase-messaging:23.1.2")
    implementation("com.google.firebase:firebase-crashlytics:18.3.6")

    //Various
    implementation("com.yandex.android:maps.mobile:4.3.1-full")
    implementation("com.google.android.gms:play-services-location:20.0.0")
    implementation("com.github.instacart.truetime-android:library:3.5")
    implementation("joda-time:joda-time:2.10.1")
    implementation("com.mikepenz:materialdrawer:7.0.0-rc08")

    //Compose
    val composeBom = platform("androidx.compose:compose-bom:2022.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.7.0")

}
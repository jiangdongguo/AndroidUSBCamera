plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = 29
    defaultConfig {
        applicationId = "com.jiangdg.ausbc"
        minSdk = 19
        targetSdk = 27
        versionCode = 126
        versionName = "3.3.3"

        ndk.abiFilters.addAll(listOf("armeabi-v7a","arm64-v8a", "x86", "x86_64"))
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("com.google.android.material:material:1.3.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.9")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.2.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.2.0")
    implementation("com.github.CymChad:BaseRecyclerViewAdapterHelper:2.9.50")
    implementation("com.afollestad.material-dialogs:core:3.2.1")

    api("com.gyf.immersionbar:immersionbar:3.0.0")
    implementation("com.github.bumptech.glide:glide:4.10.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.10.0")
    implementation("com.zlc.glide:webpdecoder:1.6.4.9.0")
    implementation("com.tencent:mmkv:1.2.12")

    // For debug online
    implementation(project(":libausbc"))
}

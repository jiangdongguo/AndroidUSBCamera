plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = "com.jiangdg.demo"

    defaultConfig {
        applicationId = "com.jiangdg.ausbc"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
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
        sourceCompatibility(libs.versions.jvmTarget.get())
        targetCompatibility(libs.versions.jvmTarget.get())
    }

    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.baseRecyclerViewAdapterHelper)
    implementation(libs.core)

    api(libs.immersionbar)
    implementation(libs.glide)
    implementation(libs.okhttp3.integration)
    implementation(libs.webpdecoder)
    implementation(libs.mmkv)

    // For debug online
    implementation(project(":libausbc"))
}

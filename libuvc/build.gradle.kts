plugins {
	id("com.android.library")
	id("kotlin-android")
}

android {
	compileSdk = 29
	namespace = "com.jiangdg.uvccamera"

	defaultConfig {
		minSdk = 19
		targetSdk = 27
		ndk.abiFilters.addAll(listOf("armeabi-v7a","arm64-v8a", "x86", "x86_64"))
	}

    compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
		}
	}

	externalNativeBuild {
		ndkBuild {
			path = File(projectDir, "./src/main/jni/Android.mk")
		}
	}
}

dependencies {
	implementation(libs.androidx.appcompat)
	implementation(libs.xlog)
}

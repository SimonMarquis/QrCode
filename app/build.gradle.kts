plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.gms.google-services")
}

val versionMajor = 1
val versionMinor = 5
val versionPatch = 0
val versionBuild = 0

android {
    compileSdkVersion(30)
    defaultConfig {
        applicationId = "fr.smarquis.qrcode"
        minSdkVersion(16)
        targetSdkVersion(30)
        versionCode = versionMajor * 1000000 + versionMinor * 10000 + versionPatch * 100 + versionBuild
        versionName = "$versionMajor.$versionMinor.$versionPatch"
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    lintOptions {
        disable("UnsafeExperimentalUsageWarning")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(Kotlin.stdlib.jdk7)

    /* Kotlin Coroutines */
    implementation(KotlinX.coroutines.core)
    implementation(KotlinX.coroutines.android)
    implementation(KotlinX.coroutines.playServices)

    implementation(AndroidX.appCompat)
    implementation(AndroidX.browser)
    implementation(AndroidX.constraintLayout)
    implementation(AndroidX.core.ktx)
    implementation(AndroidX.fragmentKtx)
    implementation(AndroidX.lifecycle.liveDataKtx)
    implementation(AndroidX.lifecycle.runtimeKtx)
    implementation(AndroidX.lifecycle.viewModelKtx)
    implementation(AndroidX.multidex)
    implementation(AndroidX.preferenceKtx)
    implementation(AndroidX.transition)

    implementation(Google.Android.material)
    implementation(Google.Android.playServices.mlKit.vision.barcodeScanning)
    implementation("com.google.zxing:core:3.4.1")

    implementation("io.fotoapparat:fotoapparat:2.7.0")

    androidTestImplementation(AndroidX.test.ext.junitKtx)
    androidTestImplementation(AndroidX.test.runner)

    /* JUnit */
    testImplementation(Testing.junit4)
}

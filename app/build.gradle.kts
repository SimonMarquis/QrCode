plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
    id("com.google.gms.google-services")
}

val versionMajor = 1
val versionMinor = 5
val versionPatch = 2
val versionBuild = 0

android {
    compileSdk = 30
    defaultConfig {
        applicationId = "fr.smarquis.qrcode"
        minSdk = 21
        targetSdk = 30
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
    lint {
        disable("GradleDependency")
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(AndroidX.activity)
    implementation(AndroidX.appCompat)
    implementation(AndroidX.browser)
    implementation(AndroidX.camera.camera2)
    implementation(AndroidX.camera.core)
    implementation(AndroidX.camera.extensions)
    implementation(AndroidX.camera.lifecycle)
    implementation(AndroidX.camera.view)
    implementation(AndroidX.constraintLayout)
    implementation(AndroidX.core.ktx)
    implementation(AndroidX.dataStore.preferences)
    implementation(AndroidX.emoji)
    implementation(AndroidX.fragmentKtx)
    implementation(AndroidX.lifecycle.liveDataKtx)
    implementation(AndroidX.lifecycle.runtimeKtx)
    implementation(AndroidX.lifecycle.viewModelKtx)
    implementation(AndroidX.multidex)
    implementation(AndroidX.preferenceKtx)
    implementation(AndroidX.transition)

    implementation(Google.Android.material)
    implementation(Google.Android.playServices.mlKit.vision.barcodeScanning)
    implementation(Google.Dagger.Hilt.android)

    implementation(Kotlin.stdlib.jdk7)
    implementation(KotlinX.coroutines.android)
    implementation(KotlinX.coroutines.core)
    implementation(KotlinX.coroutines.playServices)

    implementation("com.google.zxing:core:_")

    testImplementation(AndroidX.archCore.testing)
    testImplementation(AndroidX.test.core)
    testImplementation(AndroidX.test.coreKtx)
    testImplementation(AndroidX.test.ext.junit)
    testImplementation(AndroidX.test.runner)

    testImplementation(Kotlin.test.common)
    testImplementation(KotlinX.coroutines.test)

    testImplementation(Testing.junit4)
    testImplementation(Testing.mockK)
    testImplementation(Testing.robolectric)

    testImplementation(kotlin("reflect"))
    testImplementation(kotlin("test"))

    androidTestImplementation(AndroidX.test.ext.junitKtx)
    androidTestImplementation(AndroidX.test.runner)

    kapt(Google.dagger.hilt.compiler)
}

kapt {
    correctErrorTypes = true
}

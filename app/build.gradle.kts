plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    id("com.google.gms.google-services")
}

val versionMajor = 1
val versionMinor = 4
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
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.4.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.9")
    implementation("androidx.appcompat:appcompat:1.3.0-alpha02")
    implementation("androidx.browser:browser:1.3.0-alpha06")
    implementation("androidx.constraintlayout:constraintlayout:2.0.2")
    implementation("androidx.core:core-ktx:1.5.0-alpha04")
    implementation("androidx.fragment:fragment-ktx:1.3.0-beta01")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.3.0-beta01")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.0-beta01")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.3.0-beta01")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.preference:preference-ktx:1.1.1")

    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:16.1.2")
    implementation("com.google.android.material:material:1.3.0-alpha03")
    implementation("com.google.zxing:core:3.4.1")

    implementation("io.fotoapparat:fotoapparat:2.7.0")

    testImplementation("junit:junit:4.13.1")
    testImplementation("androidx.test.ext:junit-ktx:1.1.3-alpha02")

    androidTestImplementation("junit:junit:4.13.1")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.3-alpha02")
    androidTestImplementation("androidx.test:runner:1.3.1-alpha02")
}

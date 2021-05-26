buildscript {
    repositories {
        google()
        mavenCentral()
        /* Fix: Hilt / AGP 7.1.0-alpha01 */
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots") {
            content {
                includeModule("com.google.dagger", "hilt-android-gradle-plugin")
            }
        }
    }
    @Suppress("GradleDependency", "GradlePluginVersion")
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:_")
        classpath("com.android.tools.build:gradle:_")
        classpath(Google.playServicesGradlePlugin)
        classpath(Google.Dagger.hilt.android.gradlePlugin)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    @Suppress("GradleDependency", "GradlePluginVersion")
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:_")
        classpath("com.android.tools.build:gradle:_")
        classpath("com.google.gms:google-services:_")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io") {
            content {
                includeGroupByRegex("""com\.github\.RedApparat(\.Fotoapparat)?""")
            }
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

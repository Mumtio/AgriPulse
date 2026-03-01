import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper

plugins {
    alias(libs.plugins.android.application)
    // mention the kotlin plugin for build purposes (see note below), but don't apply it to app that is not using Kotlin
    alias(libs.plugins.kotlin.android) apply false
}

// required to workaround build issue on CI (duplicate class kotlin...) when building non-Kotlin sample apps
// all apps that use Kotlin have 'kotlin-android' plugin applied already, but plain-Java apps need this workaround to be built on CI
// KotlinAndroidPluginWrapper is translated into plugin with id == "org.jetbrains.kotlin.android" as defined here:
// https://github.com/JetBrains/kotlin/blob/master/libraries/tools/kotlin-gradle-plugin/build.gradle.kts
apply<KotlinAndroidPluginWrapper>()

android {
    namespace = "com.flir.atlassdk.acecamerasample"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.flir.atlassdk.acecamerasample"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildToolsVersion = "36.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }



    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    /**
     * Static configuration for packaging and ABI splitting.
     *
     * This configuration:
     * 1. Sets useLegacyPackaging to true - keeps the traditional APK structure which
     *    may result in more compatibility with older Android versions
     * 2. Enables ABI splitting with only arm64-v8a architecture - produces smaller APKs
     *    by including only 64-bit ARM binary code (most modern Android devices)
     */
    // Configure packaging options with static true value for useLegacyPackaging
    packaging {
        jniLibs {
            // Always use legacy packaging regardless of project properties
            useLegacyPackaging = true
        }
        dex {
            // Always use legacy packaging regardless of project properties
            useLegacyPackaging = true
        }
    }
    // Configure ABI splitting with static values
    splits {
        abi {
            // Always enable ABI splitting
            isEnable = true
            reset()
            // Only include arm64-v8a architecture
            //noinspection ChromeOsAbiSupport
            include("arm64-v8a")
            // Do not create a universal APK containing all ABIs
            isUniversalApk = false
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.jetbrains.annotations)
    implementation(libs.android.material)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Gson for JSON storage
    implementation("com.google.code.gson:gson:2.10.1")

    // FLIR SDK AAR files
    implementation(files("libs/androidsdk-release.aar"))
    implementation(files("libs/thermalsdk-release.aar"))
}

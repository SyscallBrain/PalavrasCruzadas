plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.palavrascruzadas"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "app.palavrascruzadas"
        minSdk = 26
        targetSdk = 36
        versionCode = 17
        versionName = "1.0.3"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Release signing: credentials come from env vars (CI) or Gradle properties
    // (~/.gradle/gradle.properties). Without them, release falls back to the debug key.
    val releaseStoreFile = providers.environmentVariable("SIGNING_KEYSTORE_FILE")
        .orElse(providers.gradleProperty("palavrascruzadas.keystoreFile")).orNull
    val releaseStorePassword = providers.environmentVariable("SIGNING_PASSWORD")
        .orElse(providers.gradleProperty("palavrascruzadas.keystorePassword")).orNull
    val releaseKeyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS")
        .orElse(providers.gradleProperty("palavrascruzadas.keyAlias")).orNull
    val hasReleaseSigning = releaseStoreFile != null && releaseStorePassword != null && releaseKeyAlias != null

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseStorePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName(if (hasReleaseSigning) "release" else "debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // F-Droid rejects the Google-signed dependency metadata block
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Keep native libraries unstripped so the APK is identical whether or not the
    // build machine has the NDK (needed for F-Droid reproducible builds).
    packaging {
        jniLibs {
            keepDebugSymbols += "**/*.so"
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3.windowsizeclass)
    implementation(libs.androidx.core.splashscreen)
    testImplementation(libs.junit)
}

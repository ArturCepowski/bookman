plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
}

// Q-24: push notifications need the Firebase config of the owner's project. Without the file the app still builds
// and works, just without notifications (see CLAUDE.md "Push notifications").
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// NFR-02: release builds talk HTTPS only. Production backend (Q-06, docs/deployment.md); override with
// -PbookmanApiUrl=https://... or in ~/.gradle/gradle.properties.
val releaseApiUrl = (findProperty("bookmanApiUrl") as String?) ?: "https://bookman-artur.duckdns.org/"

// Google Play needs a higher versionCode for every upload; bump both in gradle.properties (docs/deployment.md).
val appVersionCode = (findProperty("bookmanVersionCode") as String).toInt()
val appVersionName = findProperty("bookmanVersionName") as String

// Release signing (upload key). Keystore path and passwords live in ~/.gradle/gradle.properties, never in the
// repository. Without them `bundleRelease` produces an unsigned bundle and Android Studio's
// "Generate Signed App Bundle" wizard still works.
val uploadKeystore = findProperty("bookmanKeystore") as String?

android {
    namespace = "com.bookman.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bookman.app"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "API_BASE_URL", "\"$releaseApiUrl\"")
    }

    signingConfigs {
        if (uploadKeystore != null) {
            create("upload") {
                storeFile = file(uploadKeystore)
                storePassword = findProperty("bookmanKeystorePassword") as String?
                keyAlias = (findProperty("bookmanKeyAlias") as String?) ?: "upload"
                keyPassword = (findProperty("bookmanKeyPassword") as String?) ?: storePassword
            }
        }
    }

    buildTypes {
        debug {
            // Emulator alias for the developer machine running the backend locally.
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
        }
        release {
            isMinifyEnabled = false
            if (uploadKeystore != null) {
                signingConfig = signingConfigs.getByName("upload")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        // New library versions appear constantly; upgrades are deliberate, not lint failures.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.material)
    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.okhttp.mockwebserver)
}

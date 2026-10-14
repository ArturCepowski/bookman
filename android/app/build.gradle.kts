plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
}

// NFR-02: release builds talk HTTPS only. Override with -PbookmanApiUrl=https://... once hosting exists (Q-06).
val releaseApiUrl = (findProperty("bookmanApiUrl") as String?) ?: "https://bookman.invalid/"

android {
    namespace = "com.bookman.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bookman.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "API_BASE_URL", "\"$releaseApiUrl\"")
    }

    buildTypes {
        debug {
            // Emulator alias for the developer machine running the backend locally.
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
        }
        release {
            isMinifyEnabled = false
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
    implementation(libs.material)
    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.okhttp.mockwebserver)
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.zemlianikin.currency"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.zemlianikin.currency"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
    // BOM только выравнивает версии androidx Compose: без него material-ripple отстаёт от ui/foundation.
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
}

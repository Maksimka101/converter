plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvmToolchain(21)
    compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }

    android {
        namespace = "com.zemlianikin.currency.shared"
        compileSdk = 37
        minSdk = 26
        androidResources { enable = true }
    }
    jvm()

    // jvmShared — код на java.*, общий для android и desktop.
    applyDefaultHierarchyTemplate {
        common {
            group("jvmShared") {
                withJvm()
                // withAndroidTarget() не видит цель нового KMP-плагина AGP — отбираем по типу платформы.
                withCompilations { it.platformType == org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.androidJvm }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.datetime)
            implementation(libs.cmp.runtime)
            implementation(libs.cmp.foundation)
            implementation(libs.cmp.ui)
            implementation(libs.cmp.material3)
            implementation(libs.cmp.resources)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.resources {
    packageOfResClass = "com.zemlianikin.currency.shared"
}

compose.desktop {
    application {
        mainClass = "com.zemlianikin.currency.MainKt"
        // Пакеты собираются только на своей ОС (jpackage): ./gradlew :shared:packageDistributionForCurrentOS
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
            )
            packageName = "currency-converter"
            packageVersion = "1.0.0"
            // jlink сам их не находит: java.prefs — JavaPrefsStore, jdk.crypto.ec — TLS для загрузки курсов.
            modules("java.prefs", "jdk.crypto.ec")
        }
    }
}

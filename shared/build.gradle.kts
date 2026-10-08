import java.security.MessageDigest

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
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

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
            implementation(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        // Движок Ktor у каждой платформы свой, поверх её же HTTP-стека; HttpClient() находит его сам.
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.java)
        }
        wasmJsMain {
            languageSettings.optIn("kotlin.js.ExperimentalWasmJsInterop")
            dependencies {
                implementation(libs.ktor.client.js)
            }
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
            // jlink сам их не находит: java.prefs — JavaPrefsStore, java.net.http и jdk.crypto.ec — загрузка курсов.
            modules("java.prefs", "java.net.http", "jdk.crypto.ec")
        }
    }
}

// Service worker кэширует файлы по списку, а имена wasm-файлов меняются с каждой сборкой:
// список и версию (хеш содержимого) вписываем в sw.js готового дистрибутива.
mapOf(
    "wasmJsBrowserDistribution" to "productionExecutable",
    "wasmJsBrowserDevelopmentExecutableDistribution" to "developmentExecutable",
).forEach { (task, dir) ->
    val dist = layout.buildDirectory.dir("dist/wasmJs/$dir")
    tasks.named(task) {
        doLast {
            val root = dist.get().asFile
            val files = root.walkTopDown()
                .filter { it.isFile && it.name != "sw.js" && !it.name.endsWith(".map") && !it.name.endsWith(".LICENSE.txt") }
                .map { it.relativeTo(root).invariantSeparatorsPath }.sorted().toList()
            val digest = MessageDigest.getInstance("SHA-256")
            files.forEach { digest.update(it.toByteArray()); digest.update(root.resolve(it).readBytes()) }
            val version = digest.digest().take(8).joinToString("") { "%02x".format(it) }
            val sw = root.resolve("sw.js")
            val text = sw.readText()
                .replace("const VERSION = \"dev\";", "const VERSION = \"$version\";")
                .replace("const FILES = [];", "const FILES = ${files.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }};")
            check("\"$version\"" in text && "FILES = [\"" in text) { "В sw.js не нашлись строки VERSION и FILES" }
            sw.writeText(text)
        }
    }
}

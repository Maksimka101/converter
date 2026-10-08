plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
}

// В Termux Node и Binaryen, которые качает Kotlin, не запускаются (собраны под glibc) — берём системные из pkg.
// В остальных местах качаем, но из репозиториев в settings.gradle.kts: свои плагину добавлять запрещено
// (FAIL_ON_PROJECT_REPOS), поэтому его адрес загрузки обнуляем.
val termux = System.getenv("TERMUX_VERSION") != null
allprojects {
    plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec>().run {
            if (termux) download = false else downloadBaseUrl.set(null as String?)
        }
    }
    plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec>().run {
            if (termux) download = false else downloadBaseUrl.set(null as String?)
        }
    }
}

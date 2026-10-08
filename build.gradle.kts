plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
}

// В Termux Node и Binaryen, которые качает Kotlin, не запускаются (собраны под glibc) — берём системные из pkg.
if (System.getenv("TERMUX_VERSION") != null) {
    allprojects {
        plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin> {
            the<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec>().download = false
        }
        plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin> {
            the<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec>().download = false
        }
    }
}

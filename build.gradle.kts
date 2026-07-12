plugins {
    // Applied to the root project only to satisfy Gradle convention;
    // per-module configuration lives in composeApp/build.gradle.kts.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
}

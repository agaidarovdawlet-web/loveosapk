// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

allprojects {
    // Polyfill for IDE-injected scripts that expect mapPath()
    // This is often needed when using Gradle 9+ or Snap versions of Android Studio
    extra.set("mapPath", fun(path: Any): Any = path)
}

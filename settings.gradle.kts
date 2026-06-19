pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Polyfill for IDE-injected scripts that expect mapPath()
// This is often needed when using Gradle 9+ or Snap versions of Android Studio
val mapPath: (Any) -> Any = { it }
gradle.extra.set("mapPath", mapPath)

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "love os apk"
include(":app")
 

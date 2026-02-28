// Standalone settings — used by Dockerfile for isolated backend build.
// The monorepo root settings.gradle.kts includes :backend as a subproject.
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "mealplan-backend"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MealPlanPlus"
// Old pre-redesign :android + :shared (KMP) modules removed; the redesign is now the canonical :android.
include(":android")
include(":backend")

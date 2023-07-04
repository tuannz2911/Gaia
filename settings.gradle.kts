enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
    }
}

rootProject.name = "gaia"

include("gaia-api")
include("gaia-common")
include("gaia-paper")
include("gaia-fabric")

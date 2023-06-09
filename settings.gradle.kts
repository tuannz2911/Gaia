pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
    }
}

rootProject.name = "gaia"

include("gaia-core")
file("gaia-paper/nms").listFiles { _, name -> name.startsWith("nms-") }?.forEach {
    include("gaia-paper:nms:${it.name}")
}
include("gaia-paper")
include("gaia-fabric")

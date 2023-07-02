plugins {
    id("platform-conventions")
}

dependencies {
    gaiaImplementation(projects.gaiaCommon)
    project.project(":gaia-paper:nms").subprojects.forEach {
        println(it.path)
        gaiaImplementation(project(it.path)) { targetConfiguration = "reobf" }
    }
    gaiaImplementation(libs.bstats.bukkit)
    gaiaImplementation(libs.tasker.bukkit)
    gaiaImplementation(libs.configurate.hocon) {}
    gaiaImplementation(libs.cloud.paper)
    gaiaImplementation(libs.cloud.minecraft) { isTransitive = false }
    compileOnly(libs.paper)
    compileOnly(libs.worldedit.bukkit)
}

tasks {
    shadowJar {
        dependencies {
            reloc("io.leangen", "leangen")
        }
    }
    named<Copy>("processResources") {
        filesMatching("*plugin.yml") {
            expand("pluginVersion" to project.version)
        }
    }
}

gaiaPlatform {
    productionJar.set(tasks.shadowJar.flatMap { it.archiveFile })
}

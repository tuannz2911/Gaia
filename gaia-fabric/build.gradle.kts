plugins {
    alias(libs.plugins.fabric.loom)
}

dependencies {
    minecraft(libs.fabric.minecraft)
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.loader)
    modImplementation(libs.worldedit.fabric)
    modImplementation(include(libs.adventure.fabric.get())!!)
    modImplementation(include(libs.cloud.fabric.get())!!)
    implementation(include(libs.cloud.minecraft.get())!!)
    implementation(project(":gaia-core"))
    implementation(libs.tasker.fabric)
    implementation(libs.configurate.hocon)
}

loom {
    interfaceInjection.enableDependencyInterfaceInjection
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set(rootProject.name)
        destinationDirectory.set(rootProject.buildDir)
        dependencies {
            relocate("org.bstats", "me.moros.gaia.bstats")
            relocate("cloud.commandframework", "me.moros.gaia.internal.cf")
            relocate("io.leangen", "me.moros.gaia.internal.leangen")
            relocate("com.typesafe", "me.moros.gaia.internal.typesafe")
            relocate("org.spongepowered.configurate", "me.moros.gaia.internal.configurate")
        }
    }
    withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    named<Copy>("processResources") {
        filesMatching("fabric.mod.json") {
            expand("pluginVersion" to project.version)
        }
        from("../LICENSE") {
            rename { "${rootProject.name.uppercase()}_${it}" }
        }
    }
}

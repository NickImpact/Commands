plugins {
    id("impactdev.loom-conventions")
    id("impactdev.publishing-conventions")
}

architectury {
    platformSetupLoomIde()
    forge()
}

dependencies {
    forge("net.minecraftforge:forge:${rootProject.property("minecraft")}-${rootProject.property("forge")}")

    api(project(":common"))
    modApi("org.incendo:cloud-minecraft-modded-common:2.0.0-beta.1")
}

tasks {
    processResources {
        inputs.property("version", rootProject.version)

        filesMatching("fabric.mod.json") {
            expand("version" to rootProject.version)
        }
    }
}
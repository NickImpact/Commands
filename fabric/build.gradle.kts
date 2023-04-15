plugins {
    id("impactdev.loom-conventions")
    id("impactdev.publishing-conventions")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

dependencies {
    modImplementation("net.fabricmc:fabric-loader:${rootProject.property("fabric-loader")}")
    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", rootProject.property("fabric-api").toString()))

    api(project(":common"))
    modApi("cloud.commandframework:cloud-fabric:1.7.1") {
        exclude("net.fabricmc.fabric-api")
    }
}

tasks {
    processResources {
        inputs.property("version", rootProject.version)

        filesMatching("fabric.mod.json") {
            expand("version" to rootProject.version)
        }
    }
}
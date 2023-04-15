pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://repo.spongepowered.org/repository/maven-releases/")
        maven("https://repo.spongepowered.org/repository/maven-snapshots")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net/")
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    includeBuild("build-logic")
}

rootProject.name = "Commands"
include(":api")
include(":common")
include(":fabric")
include(":forge")


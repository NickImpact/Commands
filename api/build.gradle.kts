plugins {
    id("impactdev.base-conventions")
    id("impactdev.publishing-conventions")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

dependencies {
    api("com.mojang:brigadier:1.0.18")
    api("cloud.commandframework:cloud-annotations:1.7.1")
    api("cloud.commandframework:cloud-brigadier:1.7.1")
    api("cloud.commandframework:cloud-core:1.7.1")
    api("cloud.commandframework:cloud-minecraft-extras:1.7.1") {
        exclude("net.kyori")
    }

    api("net.impactdev.impactor.api:players:5.0.0-SNAPSHOT")
}
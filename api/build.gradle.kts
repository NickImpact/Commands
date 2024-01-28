plugins {
    id("impactdev.base-conventions")
    id("impactdev.publishing-conventions")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Cloud 2
    api("org.incendo:cloud-core:2.0.0-beta.1")
    api("org.incendo:cloud-annotations:2.0.0-beta.1")
    api("org.incendo:cloud-brigadier:2.0.0-beta.1")
    api("org.incendo:cloud-minecraft-extras:2.0.0-beta.1")
    api("org.incendo:cloud-processors-confirmation:1.0.0-beta.1")

    // Impactor
    api("net.impactdev.impactor.api:players:5.1.1-SNAPSHOT")
}
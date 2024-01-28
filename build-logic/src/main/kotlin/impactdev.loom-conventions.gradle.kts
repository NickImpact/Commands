plugins {
    id("impactdev.base-conventions")

    id("dev.architectury.loom")
    id("architectury-plugin")
}

architectury {
    minecraft = rootProject.property("minecraft").toString()
}

loom {
    silentMojangMappingsLicense()
}

dependencies {
    minecraft("net.minecraft:minecraft:${rootProject.property("minecraft")}")
    mappings(loom.officialMojangMappings())
}
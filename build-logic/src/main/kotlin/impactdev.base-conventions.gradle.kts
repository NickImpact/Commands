plugins {
    `java-library`
    id("org.cadixdev.licenser")
    id("net.kyori.blossom")
}

repositories {
    mavenCentral()
    maven("https://maven.impactdev.net/repository/development/")
    maven("https://libraries.minecraft.net")
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots") {
        name = "Sonatype 01 Snapshots"
    }

    maven("https://oss.sonatype.org/content/repositories/snapshots/") {
        name = "sonatype-snapshots"
        mavenContent {
            snapshotsOnly()
        }
    }
}

version = rootProject.version

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        dependsOn(updateLicenses)
        finalizedBy(test)
    }
}

license {
    header(rootProject.file("HEADER.txt"))
    properties {
        this.set("name", "ImpactDev Command Manager")
        this.set("url", "https://github.com/NickImpact/Impactor/")
        this.set("year", 2023)
    }
}
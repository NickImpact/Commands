plugins {
    id("impactdev.base-conventions")
    id("impactdev.publishing-conventions")
}

dependencies {
    api(project(":api"))
    api("net.impactdev.impactor.api:plugins:5.1.1-SNAPSHOT")

    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
}
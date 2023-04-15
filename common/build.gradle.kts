plugins {
    id("impactdev.base-conventions")
    id("impactdev.publishing-conventions")
}

dependencies {
    api(project(":api"))
    api("net.impactdev.impactor.api:plugins:5.0.0-SNAPSHOT")
}
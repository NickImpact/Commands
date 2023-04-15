plugins {
    base
}

group = "net.impactdev.impactor"
version = "${properties["plugin"]}+${properties["minecraft"]}"

val snapshot = properties["snapshot"]?.equals("true") ?: false
if(snapshot) {
    version = "$version-SNAPSHOT"
}
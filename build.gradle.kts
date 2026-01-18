import kotlin.time.Clock

plugins {
    id("java")
}

group = "com.wfee"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val hytalePath = System.getenv("HOME") + "/.local/share"
val installation = "$hytalePath/Hytale/install/release/package/game/latest"
val serverFile = file("$installation/Server/HytaleServer.jar")

dependencies {
    if ((serverFile).exists()) {
        compileOnly(files(serverFile))
    } else {
        logger.error("Hytale Server not found! ${serverFile.absolutePath}")
    }
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
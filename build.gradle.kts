plugins {
    id("java")
    id("maven-publish")
}

group = "com.wfee"
version = "1.0.0"

repositories {
    mavenCentral()
}

val hytalePath = System.getenv("HOME") + "/.local/share/Hytale"
val installation = "$hytalePath/install/release/package/game/latest"
val serverFile = "$installation/Server/HytaleServer.jar"
val modPath = "$hytalePath/UserData/Mods"

dependencies {
    if (file(installation).exists()) {
        compileOnly(files(serverFile))
    } else {
        logger.error("Hytale Installation not found! ${file(installation).absolutePath}")
    }
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    var replaceProperties = mapOf(
        "group" to project.group,
        "version" to project.version,
        "main_entrypoint" to findProperty("main_entrypoint"),
        "name" to rootProject.name,
        "author" to findProperty("author")
    )

    filesMatching("manifest.json") {
        expand(replaceProperties)
    }

    inputs.properties(replaceProperties)
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    destinationDirectory.set(file(modPath))
}
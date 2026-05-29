plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.4.0-RC2"
}

group = "me.yashbokade"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

}

kotlin {
    jvmToolchain(21)
}
tasks.jar {
    manifest {
        attributes["Main-Class"] = "me.yashbokade.MainKt"
    }
    // This line bundles all dependency files into the JAR
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
}
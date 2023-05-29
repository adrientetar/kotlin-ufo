
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version libs.versions.kotlin
    kotlin("plugin.serialization") version libs.versions.kotlin
}

group = "dev.adrientetar"
version = libs.versions.library

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation("com.googlecode.plist:dd-plist:" + libs.versions.plist.get())
    implementation("io.github.pdvrieze.xmlutil:serialization:" + libs.versions.xmlutil.get())
}

tasks.test {
    useJUnitPlatform()
}

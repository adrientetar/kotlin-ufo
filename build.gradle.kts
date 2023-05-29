
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version libs.versions.kotlin
    kotlin("plugin.serialization") version libs.versions.kotlin
    jacoco
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

    testImplementation(kotlin("test"))
    testImplementation("com.google.truth:truth:" + libs.versions.truth.get())
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.8"
    reportsDirectory.set(layout.buildDirectory.dir("jacoco"))
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
    }
    dependsOn(tasks.test)
}

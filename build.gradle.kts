plugins {
    alias(libs.plugins.kotlin.jvm)
    id("java-library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
    jacoco
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
// Pass -Prelease to publish the clean version; otherwise builds use -SNAPSHOT
// to avoid conflicting with Maven Central releases.
version = if (providers.gradleProperty("release").isPresent) VERSION_NAME else "$VERSION_NAME-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.dd.plist)
    implementation(libs.xmlutil)

    testImplementation(kotlin("test"))
    testImplementation(libs.truth)
    testImplementation(libs.jimfs)
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
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

mavenPublishing {
    publishToMavenCentral()
    // Signing is required for Maven Central, but should not block local development.
    if (providers.environmentVariable("CI").isPresent) {
        signAllPublications()
    }
}

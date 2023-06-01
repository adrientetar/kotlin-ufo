import java.net.URI

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version libs.versions.kotlin
    kotlin("plugin.serialization") version libs.versions.kotlin
    id("maven-publish")
    jacoco
}

group = "dev.adrientetar"
version = libs.versions.library.get()

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
    testImplementation("com.google.jimfs:jimfs:" + libs.versions.jimfs.get())
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.adrientetar"
            artifactId = "kotlin-ufo"
            version = libs.versions.library.get()

            from(components["java"])

            pom {
                description.set("A library to read/write UFO fonts")
                name.set("Kotlin UFO")
                url.set("https://github.com/adrientetar/kotlin-ufo")
                pom.licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                pom.scm {
                    url.set("https://github.com/adrientetar/kotlin-ufo")
                    connection.set("scm:git:git://github.com/adrientetar/kotlin-ufo.git")
                    developerConnection.set("scm:git:ssh://git@github.com/adrientetar/kotlin-ufo.git")
                }
                pom.developers {
                    developer {
                        id.set("adrientetar")
                        name.set("Adrien Tétar")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = URI("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}

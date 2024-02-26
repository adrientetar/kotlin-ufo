
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("kotlin", "1.9.22")
            version("library", "0.1.0")
            version("plist", "1.27")
            version("xmlutil", "0.86.3")

            version("jacoco", "0.8.10")
            version("jimfs", "1.2")
            version("truth", "1.4.1")
        }
    }
}

rootProject.name = "kotlin-ufo"

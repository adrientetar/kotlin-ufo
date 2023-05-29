
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("kotlin", "1.8.21")
            version("library", "0.1.0")
            version("plist", "1.26")
            version("xmlutil", "0.85.0")

            version("truth", "1.1.3")
        }
    }
}

rootProject.name = "kotlin-ufo"

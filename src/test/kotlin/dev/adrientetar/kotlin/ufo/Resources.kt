package dev.adrientetar.kotlin.ufo

import java.net.URI

internal fun getResourceURI(name: String): URI =
    checkNotNull(
        MetaInfoTests::class.java.getResource(name)
    ).toURI()

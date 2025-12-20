package io.github.adrientetar.ufo

import java.net.URI

internal fun getResourceURI(name: String): URI {
    return checkNotNull(
        MetaInfoTests::class.java.getResource(name)
    ).toURI()
}

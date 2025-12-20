package io.github.adrientetar.ufo

class UFOLibException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

package dev.adrientetar.kotlin.ufo

class UFOLibException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

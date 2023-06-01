package dev.adrientetar.kotlin.ufo

import com.dd.plist.NSDictionary

// TODO: make internal and extract an interface for the public API
class LibValues(internal val dict: NSDictionary = NSDictionary()) {
    fun containsKey(key: String): Boolean =
        dict.containsKey(key)

    fun put(key: String, value: Any) {
        dict.put(key, value)
    }

    operator fun get(key: String): Any? =
        dict[key]?.toJavaObject()

    var glyphOrder: List<String>?
        get() = dict[PUBLIC_GLYPH_ORDER]?.toList<String>()
        set(value) { dict.put(PUBLIC_GLYPH_ORDER, value) }

    companion object {
        const val PUBLIC_GLYPH_ORDER = "public.glyphOrder"
    }
}

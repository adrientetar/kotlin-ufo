package dev.adrientetar.kotlin.ufo

import com.dd.plist.NSDictionary

// TODO: make internal and extract an interface for the public API
class MetaInfoValues(private val dict: NSDictionary) {
    operator fun get(key: String): Any? =
        dict[key]?.toJavaObject()

    val creator: String
        get() = dict.getString("creator")

    val formatVersion: Int
        get() = dict.getInt("formatVersion")
}

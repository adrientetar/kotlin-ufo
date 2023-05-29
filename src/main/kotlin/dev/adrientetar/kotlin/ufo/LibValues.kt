package dev.adrientetar.kotlin.ufo

import com.dd.plist.NSDictionary

class LibValues(private val dict: NSDictionary) {
    operator fun get(key: String): Any? =
        dict[key]?.toJavaObject()
}

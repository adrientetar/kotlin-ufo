package io.github.adrientetar.ufo

import com.dd.plist.NSDictionary

class MetaInfoValues(internal val dict: NSDictionary = NSDictionary()) {
    var creator: String
        get() = dict.getString("creator")
        set(value) { dict.put("creator", value) }

    var formatVersion: Int
        get() = dict.getInt("formatVersion")
        set(value) { dict.put("formatVersion", value) }
}

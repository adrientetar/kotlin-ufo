package dev.adrientetar.kotlin.ufo

import com.dd.plist.NSObject
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal val dateTimeFormatter by lazy {
    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
        .withZone(ZoneOffset.UTC)
}

internal fun <T> NSObject.toArray() =
    toJavaObject() as Array<T>

internal fun NSObject.toStringMap() =
    toJavaObject() as Map<String, String>

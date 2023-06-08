package dev.adrientetar.kotlin.ufo

import com.dd.plist.NSArray
import com.dd.plist.NSDictionary
import com.dd.plist.NSNumber
import com.dd.plist.NSObject
import com.dd.plist.NSString
import nl.adaptivity.xmlutil.serialization.XML
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateTimeFormatter by lazy {
    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
        .withZone(ZoneOffset.UTC)
}

internal val niceXML: XML
    get() = XML {
        defaultPolicy {
            pedantic = false
            ignoreUnknownChildren()
        }
    }

internal fun NSDictionary.getInt(key: String, default: Int = 0): Int =
    (get(key) as? NSNumber)?.intValue() ?: default

internal fun NSDictionary.getIntArray(key: String): List<Int> =
    (get(key) as? NSArray)?.array?.map { (it as NSNumber).intValue() } ?: listOf()

internal fun NSDictionary.getString(key: String, default: String = ""): String =
    (get(key) as? NSString)?.content ?: default

internal fun NSDictionary.optBoolean(key: String): Boolean? =
    (get(key) as? NSNumber)?.boolValue()

internal fun NSDictionary.optFloat(key: String): Float? =
    (get(key) as? NSNumber)?.floatValue()

internal fun NSDictionary.optISO8601Date(key: String): OffsetDateTime? =
    (get(key) as? NSString)?.content?.let {
        OffsetDateTime.parse(
            it,
            dateTimeFormatter
        )
    }

internal fun NSDictionary.optInt(key: String): Int? =
    (get(key) as? NSNumber)?.intValue()

internal fun NSDictionary.optIntArray(key: String): List<Int>? =
    (get(key) as? NSArray)?.array?.map { (it as NSNumber).intValue() }

internal fun NSDictionary.optString(key: String): String? =
    (get(key) as? NSString)?.content

internal fun NSDictionary.putOpt(key: String, value: Any?) {
    when (value) {
        null -> remove(key)
        else -> put(key, value)
    }
}

internal fun NSDictionary.putOptISO8601Date(key: String, value: OffsetDateTime?) {
    putOpt(key, dateTimeFormatter.format(value))
}

internal fun NSObject.toDictionary(): NSDictionary = this as NSDictionary

internal inline fun <reified T> NSObject.toList(): List<T> =
    toList_().filterIsInstance<T>()

internal fun NSObject.toListOfListOfStrings(): List<List<String>> =
    toList_()
        .map { (it as? Array<*>)?.toList() }
        .filterIsInstance<List<String>>()

internal fun NSObject.toMapOfStrings(): Map<String, String> =
    (this as NSDictionary).hashMap
        .mapValues {
            it.value.toJavaObject() as String
        }

private fun NSObject.toList_(): List<*> =
    (
        (this as NSArray).toJavaObject() as Array<*>
    ).toList()

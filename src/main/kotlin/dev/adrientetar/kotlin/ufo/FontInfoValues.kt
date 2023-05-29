package dev.adrientetar.kotlin.ufo

import com.dd.plist.NSArray
import com.dd.plist.NSDictionary
import com.dd.plist.NSNumber
import com.dd.plist.NSString
import java.time.OffsetDateTime

// TODO: make internal and extract an interface for the public API
class FontInfoValues(private val dict: NSDictionary) {
    operator fun get(key: String): Any? =
        dict[key]?.toJavaObject()

    val ascender: Int
        get() = dict.getInt("ascender")

    val capHeight: Int
        get() = dict.getInt("capHeight")

    val copyright: String
        get() = dict.getString("copyright")

    val descender: Int
        get() = dict.getInt("descender")

    val familyName: String
        get() = dict.getString("familyName")

    val italicAngle: Float
        get() = dict.getFloat("italicAngle")

    val note: String
        get() = dict.getString("note")

    val openTypeHeadCreated: OffsetDateTime?
        get() = dict.optString("openTypeHeadCreated")?.let {
            OffsetDateTime.parse(
                it,
                dateTimeFormatter
            )
        }

    val openTypeNameDesigner: String
        get() = dict.getString("openTypeNameDesigner")

    val openTypeNameDesignerURL: String
        get() = dict.getString("openTypeNameDesignerURL")

    val openTypeNameLicense: String
        get() = dict.getString("openTypeNameLicense")

    val openTypeNameLicenseURL: String
        get() = dict.getString("openTypeNameLicenseURL")

    val openTypeNameManufacturer: String
        get() = dict.getString("openTypeNameManufacturer")

    val openTypeNameManufacturerURL: String
        get() = dict.getString("openTypeNameManufacturerURL")

    val openTypeOS2VendorID: String
        get() = dict.getString("openTypeOS2VendorID")

    val postscriptBlueValues: List<Int>
        get() = dict.getIntArray("postscriptBlueValues")

    val postscriptDefaultWidthX: Int
        get() = dict.getInt("postscriptDefaultWidthX")

    val postscriptFamilyBlues: List<Int>
        get() = dict.getIntArray("postscriptFamilyBlues")

    val postscriptFamilyOtherBlues: List<Int>
        get() = dict.getIntArray("postscriptFamilyOtherBlues")

    val postscriptOtherBlues: List<Int>
        get() = dict.getIntArray("postscriptOtherBlues")

    val postscriptStemSnapH: List<Int>
        get() = dict.getIntArray("postscriptStemSnapH")

    val postscriptStemSnapV: List<Int>
        get() = dict.getIntArray("postscriptStemSnapV")

    val styleMapFamilyName: String
        get() = dict.getString("styleMapFamilyName")

    val styleMapStyleName: String
        get() = dict.getString("styleMapStyleName")

    val styleName: String
        get() = dict.getString("styleName")

    val trademark: String
        get() = dict.getString("trademark")

    val unitsPerEm: Int
        get() = dict.getInt("unitsPerEm")

    val versionMajor: Int
        get() = dict.getInt("versionMajor")

    val versionMinor: Int
        get() = dict.getInt("versionMinor")

    val xHeight: Int
        get() = dict.getInt("xHeight")

    val year: Int
        get() = dict.getInt("year")
}

internal fun NSDictionary.getFloat(key: String, default: Float = 0f): Float =
    (get(key) as? NSNumber)?.floatValue() ?: default

internal fun NSDictionary.getInt(key: String, default: Int = 0): Int =
    (get(key) as? NSNumber)?.intValue() ?: default

internal fun NSDictionary.getIntArray(key: String): List<Int> =
    (get(key) as? NSArray)?.array?.map { (it as NSNumber).intValue() } ?: listOf()

internal fun NSDictionary.getString(key: String, default: String = ""): String =
    (get(key) as? NSString)?.content ?: default

internal fun NSDictionary.optString(key: String): String? =
    (get(key) as? NSString)?.content

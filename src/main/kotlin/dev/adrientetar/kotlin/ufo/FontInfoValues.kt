package dev.adrientetar.kotlin.ufo

import com.dd.plist.NSArray
import com.dd.plist.NSDictionary
import java.time.OffsetDateTime

// TODO: make internal and extract an interface for the public API
class FontInfoValues(internal val dict: NSDictionary = NSDictionary()) {
    fun containsKey(key: String): Boolean =
        dict.containsKey(key)

    operator fun get(key: String): Any? =
        dict[key]?.toJavaObject()

    var ascender: Int?
        get() = dict.optInt("ascender")
        set(value) { dict.putOpt("ascender", value) }

    var capHeight: Int?
        get() = dict.optInt("capHeight")
        set(value) { dict.putOpt("capHeight", value) }

    var copyright: String?
        get() = dict.optString("copyright")
        set(value) { dict.putOpt("copyright", value) }

    var descender: Int?
        get() = dict.optInt("descender")
        set(value) { dict.putOpt("descender", value) }

    var familyName: String?
        get() = dict.optString("familyName")
        set(value) { dict.putOpt("familyName", value) }

    var guidelines: List<Guideline>?
        get() = dict.optGuidelineArray("guidelines")
        set(value) { dict.putOpt("guidelines", value) }

    var italicAngle: Float?
        get() = dict.optFloat("italicAngle")
        set(value) { dict.putOpt("italicAngle", value) }

    var macintoshFONDFamilyID: Int?
        get() = dict.optInt("macintoshFONDFamilyID")
        set(value) { dict.putOpt("macintoshFONDFamilyID", value) }

    var macintoshFONDName: String?
        get() = dict.optString("macintoshFONDName")
        set(value) { dict.putOpt("macintoshFONDName", value) }

    var note: String?
        get() = dict.optString("note")
        set(value) { dict.putOpt("note", value) }

    var openTypeGaspRangeRecords: List<OpenTypeGaspRangeRecord>?
        get() = dict.optOpenTypeGaspRangeRecordArray("openTypeGaspRangeRecords")
        set(value) { dict.putOpt("openTypeGaspRangeRecords", value) }

    var openTypeHeadCreated: OffsetDateTime?
        get() = dict.optISO8601Date("openTypeHeadCreated")
        set(value) { dict.putOptISO8601Date("openTypeHeadCreated", value) }

    var openTypeHeadFlags: List<Int>?
        get() = dict.optIntArray("openTypeHeadFlags")
        set(value) { dict.putOpt("openTypeHeadFlags", value) }

    var openTypeHeadLowestRecPPEM: Int?
        get() = dict.optInt("openTypeHeadLowestRecPPEM")
        set(value) { dict.putOpt("openTypeHeadLowestRecPPEM", value) }

    var openTypeHheaAscender: Int?
        get() = dict.optInt("openTypeHheaAscender")
        set(value) { dict.putOpt("openTypeHheaAscender", value) }

    var openTypeHheaCaretOffset: Int?
        get() = dict.optInt("openTypeHheaCaretOffset")
        set(value) { dict.putOpt("openTypeHheaCaretOffset", value) }

    var openTypeHheaCaretSlopeRise: Int?
        get() = dict.optInt("openTypeHheaCaretSlopeRise")
        set(value) { dict.putOpt("openTypeHheaCaretSlopeRise", value) }

    var openTypeHheaCaretSlopeRun: Int?
        get() = dict.optInt("openTypeHheaCaretSlopeRun")
        set(value) { dict.putOpt("openTypeHheaCaretSlopeRun", value) }

    var openTypeHheaDescender: Int?
        get() = dict.optInt("openTypeHheaDescender")
        set(value) { dict.putOpt("openTypeHheaDescender", value) }

    var openTypeHheaLineGap: Int?
        get() = dict.optInt("openTypeHheaLineGap")
        set(value) { dict.putOpt("openTypeHheaLineGap", value) }

    var openTypeNameCompatibleFullName: String?
        get() = dict.optString("openTypeNameCompatibleFullName")
        set(value) { dict.putOpt("openTypeNameCompatibleFullName", value) }

    var openTypeNameDescription: String?
        get() = dict.optString("openTypeNameDescription")
        set(value) { dict.putOpt("openTypeNameDescription", value) }

    var openTypeNameDesigner: String?
        get() = dict.optString("openTypeNameDesigner")
        set(value) { dict.putOpt("openTypeNameDesigner", value) }

    var openTypeNameDesignerURL: String?
        get() = dict.optString("openTypeNameDesignerURL")
        set(value) { dict.putOpt("openTypeNameDesignerURL", value) }

    var openTypeNameLicense: String?
        get() = dict.optString("openTypeNameLicense")
        set(value) { dict.putOpt("openTypeNameLicense", value) }

    var openTypeNameLicenseURL: String?
        get() = dict.optString("openTypeNameLicenseURL")
        set(value) { dict.putOpt("openTypeNameLicenseURL", value) }

    var openTypeNameManufacturer: String?
        get() = dict.optString("openTypeNameManufacturer")
        set(value) { dict.putOpt("openTypeNameManufacturer", value) }

    var openTypeNameManufacturerURL: String?
        get() = dict.optString("openTypeNameManufacturerURL")
        set(value) { dict.putOpt("openTypeNameManufacturerURL", value) }

    var openTypeNamePreferredFamilyName: String?
        get() = dict.optString("openTypeNamePreferredFamilyName")
        set(value) { dict.putOpt("openTypeNamePreferredFamilyName", value) }

    var openTypeNamePreferredSubfamilyName: String?
        get() = dict.optString("openTypeNamePreferredSubfamilyName")
        set(value) { dict.putOpt("openTypeNamePreferredSubfamilyName", value) }

    var openTypeNameRecords: List<OpenTypeNameRecord>?
        get() = dict.optOpenTypeNameRecordArray("openTypeNameRecords")
        set(value) { dict.putOpt("openTypeNameRecords", value) }

    var openTypeNameSampleText: String?
        get() = dict.optString("openTypeNameSampleText")
        set(value) { dict.putOpt("openTypeNameSampleText", value) }

    var openTypeNameUniqueID: String?
        get() = dict.optString("openTypeNameUniqueID")
        set(value) { dict.putOpt("openTypeNameUniqueID", value) }

    var openTypeNameVersion: String?
        get() = dict.optString("openTypeNameVersion")
        set(value) { dict.putOpt("openTypeNameVersion", value) }

    var openTypeNameWWSFamilyName: String?
        get() = dict.optString("openTypeNameWWSFamilyName")
        set(value) { dict.putOpt("openTypeNameWWSFamilyName", value) }

    var openTypeNameWWSSubfamilyName: String?
        get() = dict.optString("openTypeNameWWSSubfamilyName")
        set(value) { dict.putOpt("openTypeNameWWSSubfamilyName", value) }

    var openTypeOS2CodePageRanges: List<Int>?
        get() = dict.optIntArray("openTypeOS2CodePageRanges")
        set(value) { dict.putOpt("openTypeOS2CodePageRanges", value) }

    var openTypeOS2FamilyClass: List<Int>?
        get() = dict.optIntArray("openTypeOS2FamilyClass")
        set(value) { dict.putOpt("openTypeOS2FamilyClass", value) }

    var openTypeOS2Panose: List<Int>?
        get() = dict.optIntArray("openTypeOS2Panose")
        set(value) { dict.putOpt("openTypeOS2Panose", value) }

    var openTypeOS2Selection: List<Int>?
        get() = dict.optIntArray("openTypeOS2Selection")
        set(value) { dict.putOpt("openTypeOS2Selection", value) }

    var openTypeOS2StrikeoutPosition: Int?
        get() = dict.optInt("openTypeOS2StrikeoutPosition")
        set(value) { dict.putOpt("openTypeOS2StrikeoutPosition", value) }

    var openTypeOS2StrikeoutSize: Int?
        get() = dict.optInt("openTypeOS2StrikeoutSize")
        set(value) { dict.putOpt("openTypeOS2StrikeoutSize", value) }

    var openTypeOS2SubscriptXOffset: Int?
        get() = dict.optInt("openTypeOS2SubscriptXOffset")
        set(value) { dict.putOpt("openTypeOS2SubscriptXOffset", value) }

    var openTypeOS2SubscriptXSize: Int?
        get() = dict.optInt("openTypeOS2SubscriptXSize")
        set(value) { dict.putOpt("openTypeOS2SubscriptXSize", value) }

    var openTypeOS2SubscriptYOffset: Int?
        get() = dict.optInt("openTypeOS2SubscriptYOffset")
        set(value) { dict.putOpt("openTypeOS2SubscriptYOffset", value) }

    var openTypeOS2SubscriptYSize: Int?
        get() = dict.optInt("openTypeOS2SubscriptYSize")
        set(value) { dict.putOpt("openTypeOS2SubscriptYSize", value) }

    var openTypeOS2SuperscriptXOffset: Int?
        get() = dict.optInt("openTypeOS2SuperscriptXOffset")
        set(value) { dict.putOpt("openTypeOS2SuperscriptXOffset", value) }

    var openTypeOS2SuperscriptXSize: Int?
        get() = dict.optInt("openTypeOS2SuperscriptXSize")
        set(value) { dict.putOpt("openTypeOS2SuperscriptXSize", value) }

    var openTypeOS2SuperscriptYOffset: Int?
        get() = dict.optInt("openTypeOS2SuperscriptYOffset")
        set(value) { dict.putOpt("openTypeOS2SuperscriptYOffset", value) }

    var openTypeOS2SuperscriptYSize: Int?
        get() = dict.optInt("openTypeOS2SuperscriptYSize")
        set(value) { dict.putOpt("openTypeOS2SuperscriptYSize", value) }

    var openTypeOS2Type: List<Int>?
        get() = dict.optIntArray("openTypeOS2Type")
        set(value) { dict.putOpt("openTypeOS2Type", value) }

    var openTypeOS2TypoAscender: Int?
        get() = dict.optInt("openTypeOS2TypoAscender")
        set(value) { dict.putOpt("openTypeOS2TypoAscender", value) }

    var openTypeOS2TypoDescender: Int?
        get() = dict.optInt("openTypeOS2TypoDescender")
        set(value) { dict.putOpt("openTypeOS2TypoDescender", value) }

    var openTypeOS2TypoLineGap: Int?
        get() = dict.optInt("openTypeOS2TypoLineGap")
        set(value) { dict.putOpt("openTypeOS2TypoLineGap", value) }

    var openTypeOS2UnicodeRanges: List<Int>?
        get() = dict.optIntArray("openTypeOS2UnicodeRanges")
        set(value) { dict.putOpt("openTypeOS2UnicodeRanges", value) }

    var openTypeOS2VendorID: String?
        get() = dict.optString("openTypeOS2VendorID")
        set(value) { dict.putOpt("openTypeOS2VendorID", value) }

    var openTypeOS2WeightClass: Int?
        get() = dict.optInt("openTypeOS2WeightClass")
        set(value) { dict.putOpt("openTypeOS2WeightClass", value) }

    var openTypeOS2WidthClass: Int?
        get() = dict.optInt("openTypeOS2WidthClass")
        set(value) { dict.putOpt("openTypeOS2WidthClass", value) }

    var openTypeOS2WinAscent: Int?
        get() = dict.optInt("openTypeOS2WinAscent")
        set(value) { dict.putOpt("openTypeOS2WinAscent", value) }

    var openTypeOS2WinDescent: Int?
        get() = dict.optInt("openTypeOS2WinDescent")
        set(value) { dict.putOpt("openTypeOS2WinDescent", value) }

    var openTypeVheaCaretOffset: Int?
        get() = dict.optInt("openTypeVheaCaretOffset")
        set(value) { dict.putOpt("openTypeVheaCaretOffset", value) }

    var openTypeVheaCaretSlopeRise: Int?
        get() = dict.optInt("openTypeVheaCaretSlopeRise")
        set(value) { dict.putOpt("openTypeVheaCaretSlopeRise", value) }

    var openTypeVheaCaretSlopeRun: Int?
        get() = dict.optInt("openTypeVheaCaretSlopeRun")
        set(value) { dict.putOpt("openTypeVheaCaretSlopeRun", value) }

    var openTypeVheaVertTypoAscender: Int?
        get() = dict.optInt("openTypeVheaVertTypoAscender")
        set(value) { dict.putOpt("openTypeVheaVertTypoAscender", value) }

    var openTypeVheaVertTypoDescender: Int?
        get() = dict.optInt("openTypeVheaVertTypoDescender")
        set(value) { dict.putOpt("openTypeVheaVertTypoDescender", value) }

    var openTypeVheaVertTypoLineGap: Int?
        get() = dict.optInt("openTypeVheaVertTypoLineGap")
        set(value) { dict.putOpt("openTypeVheaVertTypoLineGap", value) }

    var postscriptBlueFuzz: Int?
        get() = dict.optInt("postscriptBlueFuzz")
        set(value) { dict.putOpt("postscriptBlueFuzz", value) }

    var postscriptBlueScale: Float?
        get() = dict.optFloat("postscriptBlueScale")
        set(value) { dict.putOpt("postscriptBlueScale", value) }

    var postscriptBlueShift: Int?
        get() = dict.optInt("postscriptBlueShift")
        set(value) { dict.putOpt("postscriptBlueShift", value) }

    var postscriptBlueValues: List<Int>?
        get() = dict.optIntArray("postscriptBlueValues")
        set(value) { dict.putOpt("postscriptBlueValues", value) }

    var postscriptDefaultCharacter: String?
        get() = dict.optString("postscriptDefaultCharacter")
        set(value) { dict.putOpt("postscriptDefaultCharacter", value) }

    var postscriptDefaultWidthX: Int?
        get() = dict.optInt("postscriptDefaultWidthX")
        set(value) { dict.putOpt("postscriptDefaultWidthX", value) }

    var postscriptFamilyBlues: List<Int>?
        get() = dict.optIntArray("postscriptFamilyBlues")
        set(value) { dict.putOpt("postscriptFamilyBlues", value) }

    var postscriptFamilyOtherBlues: List<Int>?
        get() = dict.optIntArray("postscriptFamilyOtherBlues")
        set(value) { dict.putOpt("postscriptFamilyOtherBlues", value) }

    var postscriptFontName: String?
        get() = dict.optString("postscriptFontName")
        set(value) { dict.putOpt("postscriptFontName", value) }

    var postscriptForceBold: Boolean?
        get() = dict.optBoolean("postscriptForceBold")
        set(value) { dict.putOpt("postscriptForceBold", value) }

    var postscriptFullName: String?
        get() = dict.optString("postscriptFullName")
        set(value) { dict.putOpt("postscriptFullName", value) }

    var postscriptIsFixedPitch: Boolean?
        get() = dict.optBoolean("postscriptIsFixedPitch")
        set(value) { dict.putOpt("postscriptIsFixedPitch", value) }

    var postscriptNominalWidthX: Int?
        get() = dict.optInt("postscriptNominalWidthX")
        set(value) { dict.putOpt("postscriptNominalWidthX", value) }

    var postscriptOtherBlues: List<Int>?
        get() = dict.optIntArray("postscriptOtherBlues")
        set(value) { dict.putOpt("postscriptOtherBlues", value) }

    var postscriptSlantAngle: Float?
        get() = dict.optFloat("postscriptSlantAngle")
        set(value) { dict.putOpt("postscriptSlantAngle", value) }

    var postscriptStemSnapH: List<Int>?
        get() = dict.optIntArray("postscriptStemSnapH")
        set(value) { dict.putOpt("postscriptStemSnapH", value) }

    var postscriptStemSnapV: List<Int>?
        get() = dict.optIntArray("postscriptStemSnapV")
        set(value) { dict.putOpt("postscriptStemSnapV", value) }

    var postscriptUnderlinePosition: Int?
        get() = dict.optInt("postscriptUnderlinePosition")
        set(value) { dict.putOpt("postscriptUnderlinePosition", value) }

    var postscriptUnderlineThickness: Int?
        get() = dict.optInt("postscriptUnderlineThickness")
        set(value) { dict.putOpt("postscriptUnderlineThickness", value) }

    var postscriptUniqueID: Int?
        get() = dict.optInt("postscriptUniqueID")
        set(value) { dict.putOpt("postscriptUniqueID", value) }

    var postscriptWeightName: String?
        get() = dict.optString("postscriptWeightName")
        set(value) { dict.putOpt("postscriptWeightName", value) }

    var postscriptWindowsCharacterSet: Int?
        get() = dict.optInt("postscriptWindowsCharacterSet")
        set(value) { dict.putOpt("postscriptWindowsCharacterSet", value) }

    var styleMapFamilyName: String?
        get() = dict.optString("styleMapFamilyName")
        set(value) { dict.putOpt("styleMapFamilyName", value) }

    var styleMapStyleName: String?
        get() = dict.optString("styleMapStyleName")
        set(value) { dict.putOpt("styleMapStyleName", value) }

    var styleName: String?
        get() = dict.optString("styleName")
        set(value) { dict.putOpt("styleName", value) }

    var trademark: String?
        get() = dict.optString("trademark")
        set(value) { dict.putOpt("trademark", value) }

    var unitsPerEm: Int?
        get() = dict.optInt("unitsPerEm")
        set(value) { dict.putOpt("unitsPerEm", value) }

    var versionMajor: Int?
        get() = dict.optInt("versionMajor")
        set(value) { dict.putOpt("versionMajor", value) }

    var versionMinor: Int?
        get() = dict.optInt("versionMinor")
        set(value) { dict.putOpt("versionMinor", value) }

    var xHeight: Int?
        get() = dict.optInt("xHeight")
        set(value) { dict.putOpt("xHeight", value) }

    var year: Int?
        get() = dict.optInt("year")
        set(value) { dict.putOpt("year", value) }
}

data class Guideline(
    val x: Int? = null,
    val y: Int? = null,
    val angle: Float? = null,
    val name: String? = null,
    // TODO: we could parse this to a color type
    val color: String? = null,
    val identifier: String? = null
)

data class OpenTypeGaspRangeRecord(
    val rangeMaxPPEM: Int,
    // TODO: we could parse this to an enum set
    val rangeGaspBehavior: List<Int>
)

data class OpenTypeNameRecord(
    val nameID: Int,
    val platformID: Int,
    val encodingID: Int,
    val languageID: Int,
    val string: String
)

internal fun NSDictionary.optGuidelineArray(key: String): List<Guideline>? =
    (get(key) as? NSArray)?.array?.map {
        with (it as NSDictionary) {
            Guideline(
                x = optInt("x"),
                y = optInt("y"),
                angle = optFloat("angle"),
                name = optString("name"),
                color = optString("color"),
                identifier = optString("identifier")
            )
        }
    }

internal fun NSDictionary.optOpenTypeGaspRangeRecordArray(key: String): List<OpenTypeGaspRangeRecord>? =
    (get(key) as? NSArray)?.array?.map {
        with (it as NSDictionary) {
            OpenTypeGaspRangeRecord(
                rangeMaxPPEM = getInt("rangeMaxPPEM"),
                rangeGaspBehavior = getIntArray("rangeGaspBehavior")
            )
        }
    }

internal fun NSDictionary.optOpenTypeNameRecordArray(key: String): List<OpenTypeNameRecord>? =
    (get(key) as? NSArray)?.array?.map {
        with (it as NSDictionary) {
            OpenTypeNameRecord(
                nameID = getInt("nameID"),
                platformID = getInt("platformID"),
                encodingID = getInt("encodingID"),
                languageID = getInt("languageID"),
                string = getString("string")
            )
        }
    }

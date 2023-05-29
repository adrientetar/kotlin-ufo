package dev.adrientetar.kotlin.ufo

import com.dd.plist.NSArray
import com.dd.plist.NSDictionary
import com.dd.plist.NSNumber
import com.dd.plist.NSString
import com.dd.plist.XMLPropertyListWriter
import java.nio.file.Files
import java.nio.file.Path
import java.time.OffsetDateTime

/**
 * UFO font writer, with [ufo] as its path.
 */
class UFOWriter(private val ufo: Path) {
    init {
        // TODO: clear out the folder, if it exists

        // This will throw if `ufo` exists and is not a directory
        Files.createDirectories(ufo)
    }

    fun writeMetaInfo() {
        val dict = NSDictionary().apply {
            put("creator", "dev.adrientetar.kotlin.ufo")
            put("formatVersion", 3)
        }
        XMLPropertyListWriter.write(dict, ufo.resolve("metainfo.plist"))
    }

    fun writeFontInfo(
        ascender: Int,
        blueValues: List<Int>,
        capHeight: Int,
        copyright: String,
        date: OffsetDateTime,
        descender: Int,
        designer: String,
        designerURL: String,
        familyName: String,
        italicAngle: Float,
        manufacturer: String,
        manufacturerURL: String,
        otherBlues: List<Int>,
        styleName: String,
        unitsPerEm: Int,
        versionMajor: Int,
        versionMinor: Int,
        xHeight: Int,
    ) {
        val dict = NSDictionary().apply {
            put("familyName", familyName)
            put("copyright", copyright)
            put("openTypeHeadCreated", dateTimeFormatter.format(date))
            put("openTypeNameDesigner", designer)
            put("openTypeNameDesignerURL", designerURL)
            put("openTypeNameManufacturer", manufacturer)
            put("openTypeNameManufacturerURL", manufacturerURL)
            put("unitsPerEm", unitsPerEm)
            put("versionMajor", versionMajor)
            put("versionMinor", versionMinor)

            put("styleName", styleName)
            put("ascender", ascender)
            put("capHeight", capHeight)
            put("descender", descender)
            put("italicAngle", italicAngle)
            put("xHeight", xHeight)

            put(
                "postscriptBlueValues",
                NSArray(*blueValues.map { NSNumber(it) }.toTypedArray())
            )
            put(
                "postscriptOtherBlues",
                NSArray(*otherBlues.map { NSNumber(it) }.toTypedArray())
            )
        }

        XMLPropertyListWriter.write(dict, ufo.resolve("fontinfo.plist"))
    }

    fun writeGlyphs() {
        // Write layercontents
        run {
            val layerContentsPath = ufo.resolve("layercontents.plist")
            val array = NSArray(
                NSArray(
                    NSString("foreground"),
                    NSString("glyphs")
                )
            )

            XMLPropertyListWriter.write(array, ufo.resolve("fontinfo.plist"))
        }

        // Create glyphs/ and write contents.plist
        val glyphsPath = ufo.resolve("glyphs/")
        Files.createDirectories(glyphsPath)

        // TODO: write glyphs/contents.plist
    }

    fun writeLib(glyphOrder: List<String>) {
        val dict = NSDictionary().apply {
            put(
                "public.glyphOrder",
                glyphOrder
            )
        }

        XMLPropertyListWriter.write(dict, ufo.resolve("lib.plist"))
    }
}

private fun String.toUFOFileName(): String {
    val filtered = buildString {
        val startsWithDot = this[0] == '.'
        if (startsWithDot) {
            append("_")
        }
        for (ch in drop(if (startsWithDot) 1 else 0)) {
            when {
                ch in illegalCharacters -> append("_")
                ch != ch.lowercaseChar() -> append("${ch}_")
                else -> append(ch)
            }
        }
    }.take(255)
    return filtered
}

// Restrictions are taken mostly from
// https://docs.microsoft.com/en-gb/windows/win32/fileio/naming-a-file#naming-conventions.
//
// 1. Integer value zero, sometimes referred to as the ASCII NUL character.
// 2. Characters whose integer representations are in the range 1 to 31,
//    inclusive.
// 3. Various characters that (mostly) Windows and POSIX-y filesystems don't
//    allow, plus "(" and ")", as per the specification.
private val illegalCharacters = setOf(
    '\u0000',
    '\u0001',
    '\u0002',
    '\u0003',
    '\u0004',
    '\u0005',
    '\u0006',
    '\u0007',
    '\u0008',
    '\t',
    '\n',
    '\u000b',
    '\u000c',
    '\r',
    '\u000e',
    '\u000f',
    '\u0010',
    '\u0011',
    '\u0012',
    '\u0013',
    '\u0014',
    '\u0015',
    '\u0016',
    '\u0017',
    '\u0018',
    '\u0019',
    '\u001a',
    '\u001b',
    '\u001c',
    '\u001d',
    '\u001e',
    '\u001f',
    '"',
    '*',
    '+',
    '/',
    ':',
    '<',
    '>',
    '?',
    '[',
    '\\',
    ']',
    '(',
    ')',
    '|',
    '\u007f',
)

private val reservedFileNames = setOf(
    "aux",
    "clock$",
    "com1",
    "com2",
    "com3",
    "com4",
    "com5",
    "com6",
    "com7",
    "com8",
    "com9",
    "con",
    "lpt1",
    "lpt2",
    "lpt3",
    "lpt4",
    "lpt5",
    "lpt6",
    "lpt7",
    "lpt8",
    "lpt9",
    "nul",
    "prn",
)

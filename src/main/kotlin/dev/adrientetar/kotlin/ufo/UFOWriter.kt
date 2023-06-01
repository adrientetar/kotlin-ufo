package dev.adrientetar.kotlin.ufo

import com.dd.plist.NSArray
import com.dd.plist.NSObject
import com.dd.plist.NSString
import com.dd.plist.XMLPropertyListWriter
import kotlinx.serialization.encodeToString
import java.nio.file.Files
import java.nio.file.Path

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
        val values = MetaInfoValues().apply {
            creator = "dev.adrientetar.kotlin.ufo"
            formatVersion = 3
        }
        XMLPropertyListWriter.write(values.dict, ufo.resolve("metainfo.plist"))
    }

    fun writeFontInfo(values: FontInfoValues) {
        XMLPropertyListWriter.write(values.dict, ufo.resolve("fontinfo.plist"))
    }

    fun writeGlyphs(glyphs: List<GlyphValues>) {
        // Write layercontents.plist
        run {
            val layerContentsPath = ufo.resolve("layercontents.plist")
            val array = NSArray(
                NSArray(
                    NSString("foreground"),
                    NSString("glyphs")
                )
            )

            XMLPropertyListWriter.write(array, layerContentsPath)
        }

        // Create glyphs/ and write contents.plist
        val glyphsDir = ufo.resolve("glyphs/")
        Files.createDirectories(glyphsDir)

        val contentsPath = glyphsDir.resolve("contents.plist")
        val contentsDict = glyphs.associateBy(
            { it.name },
            { it.name?.toGLIFFileName() }
        )
        XMLPropertyListWriter.write(
            NSObject.fromJavaObject(contentsDict),
            contentsPath
        )

        // Write the GLIF files
        for (glyph in glyphs) {
            val fileName = checkNotNull(contentsDict[glyph.name])
            val glifPath = glyphsDir.resolve(fileName)

            val content = niceXML.encodeToString(glyph.glif)
            Files.writeString(glifPath, content)
        }
    }

    fun writeLib(values: LibValues) {
        XMLPropertyListWriter.write(values.dict, ufo.resolve("lib.plist"))
    }
}

// TODO: add existing file names parameter
private fun String.toGLIFFileName(): String {
    val b = StringBuilder(length)
    val startsWithDot = isNotEmpty() && this[0] == '.'

    if (startsWithDot) {
        b.append("_")
    }
    for (ch in drop(if (startsWithDot) 1 else 0)) {
        when {
            ch in illegalCharacters -> b.append("_")
            ch != ch.lowercaseChar() -> b.append("${ch}_")
            else -> b.append(ch)
        }
    }

    return b.toString().take(255)
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

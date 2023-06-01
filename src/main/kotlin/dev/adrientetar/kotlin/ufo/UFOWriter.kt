package dev.adrientetar.kotlin.ufo

import com.dd.plist.NSArray
import com.dd.plist.NSObject
import com.dd.plist.NSString
import com.dd.plist.XMLPropertyListWriter
import kotlinx.serialization.encodeToString
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.name

/**
 * UFO font writer.
 *
 * @param ufo The path to write the UFO to
 * @param clearDirectory Whether to clear the path and create an empty folder,
 *  see [clearDirectory]. Defaults to true
 */
// TODO: look into doing incremental write (without clearing all previous contents)
class UFOWriter(
    private val ufo: Path,
    clearDirectory: Boolean = true
) {
    init {
        if (clearDirectory) {
            clearDirectory()
        }
    }

    @OptIn(ExperimentalPathApi::class)
    fun clearDirectory() {
        // Clear out the folder, if it exists
        try {
            ufo.deleteRecursively()
        } catch (ex: IOException) {
            throw UFOLibException("Failed to remove existing UFO content", ex)
        }

        // Create directory
        try {
            Files.createDirectories(ufo)
        } catch (ex: IOException) {
            throw UFOLibException("Failed to create UFO directory", ex)
        }
    }

    fun writeFontInfo(values: FontInfoValues) {
        val path = ufo.resolve("fontinfo.plist")
        path.writePlist(values.dict)
    }

    fun writeGlyphs(glyphs: List<GlyphValues>) {
        // Gather name to filename mapping
        val contentsDict = glyphs.associateBy(
            { it.name },
            { it.name?.toFileName() }
        )

        // Write layercontents.plist
        run {
            val array = NSArray(
                NSArray(
                    NSString("foreground"),
                    NSString("glyphs")
                )
            )
            ufo.resolve("layercontents.plist").writePlist(array)
        }

        // Create glyphs/ and write contents.plist
        val glyphsDir = ufo.resolve("glyphs")
        try {
            Files.createDirectories(glyphsDir)
        } catch (ex: IOException) {
            throw UFOLibException("Failed to create glyphs directory", ex)
        }

        run {
            val root = NSObject.fromJavaObject(contentsDict)
            glyphsDir.resolve("contents.plist").writePlist(root)
        }

        // Write the GLIF files
        for (glyph in glyphs) {
            val fileName = checkNotNull(contentsDict[glyph.name])
            val glifPath = glyphsDir.resolve(fileName)

            glifPath.writeXML(glyph.glif)
        }
    }

    fun writeLib(values: LibValues) {
        val path = ufo.resolve("lib.plist")
        path.writePlist(values.dict)
    }

    fun writeMetaInfo() {
        val values = MetaInfoValues().apply {
            creator = "dev.adrientetar.kotlin.ufo"
            formatVersion = 3
        }
        val path = ufo.resolve("metainfo.plist")

        path.writePlist(values.dict)
    }

    private fun Path.writePlist(root: NSObject) {
        try {
            XMLPropertyListWriter.write(root, this)
        } catch (ex: Exception) {
            throw UFOLibException("Failed to write $name", ex)
        }
    }

    private inline fun <reified T> Path.writeXML(value: T) =
        try {
            val content = niceXML.encodeToString(value)
            Files.writeString(this, content)
        } catch (ex: Exception) {
            throw UFOLibException("Failed to write $name", ex)
        }
}

// TODO: add existing file names parameter
internal fun String.toFileName(): String {
    val filtered = run {
        val b = StringBuilder(length)

        // Replace an initial period with '_'
        val startsWithDot = isNotEmpty() && this[0] == '.'
        if (startsWithDot) {
            b.append('_')
        }

        // Filter characters and limit length to 255
        for (ch in drop(if (startsWithDot) 1 else 0)) {
            when {
                ch in illegalCharacters -> b.append('_')
                ch != ch.lowercaseChar() -> b.append("${ch}_")
                else -> b.append(ch)
            }
        }
        b.toString().take(255)
    }
    // Test for illegal file names
    val result = filtered
        .split(".")
        .joinToString(".") { part ->
            when (part) {
                in reservedFileNames -> "_$part"
                else -> part
            }
        }

    return result
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

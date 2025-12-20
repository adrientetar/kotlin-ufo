package io.github.adrientetar.ufo

import com.dd.plist.NSArray
import com.dd.plist.NSDictionary
import com.dd.plist.NSObject
import com.dd.plist.NSString
import com.dd.plist.XMLPropertyListWriter
import kotlinx.serialization.encodeToString
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.name
import kotlin.io.path.writeText

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

    /**
     * Writes multiple layers to the UFO.
     *
     * This writes layercontents.plist and all layer directories with their glyphs.
     * The first layer in the list should be the default layer.
     *
     * @param layers The layers to write, in order from top to bottom
     */
    fun writeLayers(layers: List<Layer>) {
        if (layers.isEmpty()) return

        // Write layercontents.plist
        val layerContentsArray = NSArray(*layers.map { layer ->
            NSArray(NSString(layer.name), NSString(layer.directoryName))
        }.toTypedArray())
        ufo.resolve("layercontents.plist").writePlist(layerContentsArray)

        // Write each layer
        for (layer in layers) {
            writeLayerGlyphs(layer)
        }
    }

    /**
     * Writes a single layer's glyphs to its directory.
     *
     * @param layer The layer to write
     */
    fun writeLayerGlyphs(layer: Layer) {
        val glyphsDir = ufo.resolve(layer.directoryName)

        // Gather name to filename mapping (with .glif extension)
        val contentsDict = layer.glyphs.associateBy(
            { it.name },
            { it.name?.toFileName()?.let { "$it.glif" } }
        )

        // Create layer directory and write contents.plist
        try {
            Files.createDirectories(glyphsDir)
        } catch (ex: IOException) {
            throw UFOLibException("Failed to create layer directory: ${layer.directoryName}", ex)
        }

        run {
            val root = NSObject.fromJavaObject(contentsDict)
            glyphsDir.resolve("contents.plist").writePlist(root)
        }

        // Write layerinfo.plist if layer has info
        val layerInfo = layer.info
        if (layerInfo != null && !layerInfo.isEmpty) {
            val layerInfoDict = LayerInfo.toDictionary(layerInfo)
            glyphsDir.resolve("layerinfo.plist").writePlist(layerInfoDict)
        } else {
            // Delete existing layerinfo.plist if info is empty
            glyphsDir.resolve("layerinfo.plist").deleteIfExists()
        }

        // Write the GLIF files
        for (glyph in layer.glyphs) {
            val fileName = checkNotNull(contentsDict[glyph.name])
            val glifPath = glyphsDir.resolve(fileName)
            glifPath.writeGlif(glyph.glif)
        }
    }

    /**
     * Writes glyphs to the default foreground layer.
     *
     * This is a convenience method that creates a default layer and writes it.
     * For multi-layer support, use [writeLayers] instead.
     */
    fun writeGlyphs(glyphs: List<GlyphValues>) {
        writeLayers(listOf(Layer.createDefault(glyphs)))
    }

    fun writeGroups(values: GroupsValues) {
        val path = ufo.resolve("groups.plist")
        if (values.dict.count() == 0) {
            path.deleteIfExists()
            return
        }
        path.writePlist(values.dict)
    }

    fun writeKerning(values: KerningValues) {
        val path = ufo.resolve("kerning.plist")
        if (values.dict.count() == 0) {
            path.deleteIfExists()
            return
        }
        path.writePlist(values.dict)
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

    /**
     * Writes the OpenType feature definitions to features.fea.
     *
     * If the features text is null or blank, any existing file will be deleted.
     *
     * @param values The FeaturesValues containing the feature text to write
     */
    fun writeFeatures(values: FeaturesValues) {
        val path = ufo.resolve("features.fea")
        if (values.isEmpty) {
            path.deleteIfExists()
            return
        }
        path.writeText(values.text!!)
    }

    /**
     * Returns an accessor for the images/ directory.
     *
     * The images directory contains PNG images that can be referenced by glyph image elements.
     * Use this to write, read, or manage image files.
     *
     * @return ImagesDirectory instance for writing/managing images
     */
    fun images(): ImagesDirectory {
        return ImagesDirectory(ufo.resolve(ImagesDirectory.DIRECTORY_NAME))
    }

    /**
     * Returns an accessor for the data/ directory.
     *
     * The data directory allows authoring tools to store application-specific data
     * that is too complex or too large for lib.plist.
     *
     * @return DataDirectory instance for writing/managing data files
     */
    fun data(): DataDirectory {
        return DataDirectory(ufo.resolve(DataDirectory.DIRECTORY_NAME))
    }

    private fun Path.writePlist(root: NSObject) {
        try {
            XMLPropertyListWriter.write(root, this)
        } catch (ex: Exception) {
            throw UFOLibException("Failed to write $name", ex)
        }
    }

    private fun Path.writeGlif(glif: Glif) {
        try {
            var content = niceXML.encodeToString(glif)
            
            // Append lib element if present and non-empty
            val lib = glif.lib
            if (lib != null && lib.content.count() > 0) {
                val libXml = serializeLibToXml(lib.content)
                // Insert lib before closing </glyph> tag
                content = content.replace("</glyph>", "$libXml\n</glyph>")
            }
            
            Files.writeString(this, content)
        } catch (ex: Exception) {
            throw UFOLibException("Failed to write $name", ex)
        }
    }

    private inline fun <reified T> Path.writeXML(value: T) {
        try {
            val content = niceXML.encodeToString(value)
            Files.writeString(this, content)
        } catch (ex: Exception) {
            throw UFOLibException("Failed to write $name", ex)
        }
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

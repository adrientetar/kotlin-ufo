package dev.adrientetar.kotlin.ufo

import com.dd.plist.NSDictionary
import com.dd.plist.NSObject
import com.dd.plist.PropertyListParser
import kotlinx.serialization.decodeFromString
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.readText

// TODO: check metainfo formatVersion?
// TODO: add Log statements

/**
 * UFO font reader, with [ufo] as its path.
 *
 * If [strict] is true, the functions in this class may throw [UFOLibException] if the file doesn't
 * match the file format structure. Otherwise, errors will be silently skipped.
 */
class UFOReader(
    private val ufo: Path,
    private val strict: Boolean = true
) {
    fun readFontInfo(): FontInfoValues {
        val path = ufo.resolve("fontinfo.plist")
        val fontInfoDict = path.readPlist(NSObject::toDictionary)

        return FontInfoValues(fontInfoDict ?: NSDictionary())
    }

    fun readGlyphs(): Sequence<GlyphValues> {
        // Read foreground layer name
        val layerContents = ufo.resolve("layercontents.plist")
            .readPlist(NSObject::toListOfListOfStrings) ?: return sequenceOf()
        val foregroundDir = run {
            val name = layerContents.firstOrNull()?.lastOrNull()
            name?.plus("/") ?: "glyphs/"
        }

        // Read glyph names according to glyph order
        val contentsDict = ufo.resolve(foregroundDir + "contents.plist")
            .readPlist(NSObject::toMapOfStrings) ?: return sequenceOf()
        // TODO: reuse previous parse?
        val lib = readLib()
        // TODO: should we use the order from contents.plist as well?
        val glyphNames = ufoGlyphOrder(
            contentsDict.keys,
            lib.glyphOrder
        )

        return sequence {
            // `mapNotNull` will protect us against missing glyphs in the glyph order
            for (glifFileName in glyphNames.mapNotNull { contentsDict[it] }) {
                val glifPath = ufo.resolve(foregroundDir + glifFileName)
                val glif = glifPath.readXML<Glif>()

                if (glif != null) {
                    yield(GlyphValues(glif))
                }
            }
        }
    }

    // One thing we can do is return an object in readGlyphs() that can both have an iterator
    // with all the glyphs and also query a specific one.
    //fun readGlyph(name: String): GlyphValues = TODO()

    fun readLib(): LibValues {
        val path = ufo.resolve("lib.plist")
        val libDict = path.readPlist(NSObject::toDictionary, required = false)

        return LibValues(libDict ?: NSDictionary())
    }

    fun readMetaInfo(): MetaInfoValues {
        val path = ufo.resolve("metainfo.plist")
        val metaInfoDict = path.readPlist(NSObject::toDictionary)

        return MetaInfoValues(metaInfoDict ?: NSDictionary())
    }

    private fun <T> Path.readPlist(transform: (NSObject) -> T, required: Boolean = true): T? =
        try {
            transform(PropertyListParser.parse(this))
        } catch (ex: Exception) {
            if (strict && (required || ex !is NoSuchFileException)) {
                throw UFOLibException("Failed to read $name", ex)
            }
            null
        }

    private inline fun <reified T> Path.readXML(): T? =
        try {
            niceXML.decodeFromString<T>(readText())
        } catch (ex: Exception) {
            if (strict) {
                throw UFOLibException("Failed to read $name", ex)
            }
            null
        }
}

internal fun ufoGlyphOrder(allGlyphs: Collection<String>, glyphOrder: List<String>?): Collection<String> =
    when (glyphOrder) {
        null -> allGlyphs
        else -> {
            val glyphOrderSet = glyphOrder.toSet()
            val allGlyphsSet = allGlyphs.toSet()

            if (glyphOrderSet.containsAll(allGlyphsSet)) {
                glyphOrder
            } else {
                val missingGlyphs = allGlyphsSet - glyphOrderSet
                glyphOrder.plus(missingGlyphs)
            }
        }
    }

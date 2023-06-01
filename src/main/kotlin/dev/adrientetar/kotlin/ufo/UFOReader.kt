package dev.adrientetar.kotlin.ufo

import com.dd.plist.NSDictionary
import com.dd.plist.PropertyListParser
import kotlinx.serialization.decodeFromString
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.readText

// TODO: check metainfo formatVersion?
// TODO: add Log statements

/**
 * UFO font reader, with [ufo] as its path.
 */
class UFOReader(private val ufo: Path) {
    fun readMetaInfo(): MetaInfoValues {
        val metaInfoDict = PropertyListParser.parse(
            ufo.resolve("metainfo.plist")
        ) as NSDictionary

        return MetaInfoValues(metaInfoDict)
    }

    fun readFontInfo(): FontInfoValues {
        val fontInfoDict = PropertyListParser.parse(
            ufo.resolve("fontinfo.plist")
        ) as NSDictionary

        return FontInfoValues(fontInfoDict)
    }

    fun readGlyphs(): Sequence<GlyphValues> {
        // Read foreground layer name
        val layerContents = PropertyListParser.parse(
            ufo.resolve("layercontents.plist")
        ).toListOfList<String>() ?: return sequenceOf()
        val foregroundLayerFolder = run {
            val name = layerContents.firstOrNull()?.lastOrNull()
            name?.plus("/") ?: "glyphs/"
        }

        // Read glyph names according to glyph order
        val contentsDict = PropertyListParser.parse(
            ufo.resolve(foregroundLayerFolder + "contents.plist")
        ).toStringMap() ?: return sequenceOf()
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
                val glifPath = ufo.resolve(foregroundLayerFolder + glifFileName)
                val glif = niceXML.decodeFromString<Glif>(
                    glifPath.readText()
                )
                yield(GlyphValues(glif))
            }
        }
    }

    fun readGlyph(name: String): GlyphValues = TODO()

    fun readLib(): LibValues {
        val libDict = try {
            PropertyListParser.parse(
                ufo.resolve("lib.plist")
            ) as? NSDictionary
        } catch (ex: NoSuchFileException) {
            null
        }

        return LibValues(libDict ?: NSDictionary())
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

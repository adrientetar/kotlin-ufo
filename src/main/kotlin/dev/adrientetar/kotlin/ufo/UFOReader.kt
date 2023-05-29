package dev.adrientetar.kotlin.ufo

import com.dd.plist.NSDictionary
import com.dd.plist.PropertyListParser
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.serialization.XML
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
        ).toArray<Array<String>>()
        val foregroundLayerFolder =
            layerContents.firstOrNull()?.lastOrNull()?.plus("/") ?: "glyphs/"

        // Read glyph names according to glyph order
        val glifDict = PropertyListParser.parse(
            ufo.resolve(foregroundLayerFolder + "contents.plist")
        ).toStringMap()
        // TODO: reuse previous parse
        val lib = readLib()
        val glyphNames = ufoGlyphOrder(
            glifDict.keys,
            lib["public.glyphOrder"] as? List<String>
        )

        return sequence {
            for (glifFileName in glyphNames.map { glifDict[it] }) {
                val glif = niceXML.decodeFromString<Glif>(
                    ufo.resolve(foregroundLayerFolder + glifFileName).readText()
                )

                yield(GlyphValues(glif))
            }
        }
    }

    fun readGlyph(name: String): GlyphValues = TODO()

    fun readLib(): LibValues {
        val libDict = PropertyListParser.parse(
            ufo.resolve("lib.plist")
        ) as NSDictionary

        return LibValues(libDict)
    }
}

internal val niceXML: XML
    get() = XML {
        defaultPolicy {
            pedantic = false
            ignoreUnknownChildren()
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

package io.github.adrientetar.ufo

import com.dd.plist.NSDictionary
import com.dd.plist.NSObject
import com.dd.plist.XMLPropertyListParser
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

    /**
     * Reads the layer contents mapping from layercontents.plist.
     *
     * @return List of pairs where each pair is (layerName, directoryName)
     */
    fun readLayerContents(): List<Pair<String, String>> {
        val layerContents = ufo.resolve("layercontents.plist")
            .readPlist(NSObject::toListOfListOfStrings) ?: return listOf(
                Layer.DEFAULT_NAME to Layer.DEFAULT_DIRECTORY
            )

        return layerContents.mapNotNull { entry ->
            if (entry.size >= 2) {
                entry[0] to entry[1]
            } else null
        }
    }

    /**
     * Returns the names of all layers in this UFO.
     */
    fun layerNames(): List<String> = readLayerContents().map { it.first }

    /**
     * Reads layer info from a layer directory.
     */
    fun readLayerInfo(directoryName: String): LayerInfo? {
        val path = ufo.resolve(directoryName).resolve("layerinfo.plist")
        val dict = path.readPlist(NSObject::toDictionary, required = false) ?: return null
        return LayerInfo.fromDictionary(dict)
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
        val glyphNames = ufoGlyphOrder(
            contentsDict.keys,
            lib.glyphOrder
        )

        return sequence {
            // `mapNotNull` will protect us against missing glyphs in the glyph order
            for (glifFileName in glyphNames.mapNotNull { contentsDict[it] }) {
                val glifPath = ufo.resolve(foregroundDir + glifFileName)
                val glifXml = glifPath.readTextOrNull()
                val glif = glifXml?.let { niceXML.decodeFromString<Glif>(it) }

                if (glif != null) {
                    // Parse the lib separately since xmlutil has trouble with nested plist
                    if (glifXml.contains("<lib>")) {
                        val libDict = extractLibFromGlifXml(glifXml)
                        if (libDict.count() > 0) {
                            glif.lib = GlifLib(libDict)
                        }
                    }
                    yield(GlyphValues(glif))
                }
            }
        }
    }

    /**
     * Returns a lazy-loading glyph set for the default (foreground) layer.
     *
     * Unlike [readGlyphs], this does not load all glyphs into memory at once.
     * Glyphs are loaded on-demand when accessed, making it suitable for large fonts.
     *
     * Example:
     * ```kotlin
     * val glyphSet = reader.getGlyphSet()
     * val aGlyph = glyphSet["a"]  // Only loads 'a'
     * val bGlyph = glyphSet["b"]  // Only loads 'b'
     * ```
     *
     * @return A [GlyphSet] for lazy glyph access
     */
    fun getGlyphSet(): GlyphSet {
        val layerContents = readLayerContents()
        val defaultEntry = layerContents.firstOrNull() ?: (Layer.DEFAULT_NAME to Layer.DEFAULT_DIRECTORY)
        val lib = readLib()

        return GlyphSet(
            layerDirectory = ufo.resolve(defaultEntry.second),
            glyphOrder = lib.glyphOrder,
            strict = strict
        )
    }

    /**
     * Returns a lazy-loading glyph set for a specific layer.
     *
     * @param layerName The name of the layer (e.g., "public.default", "public.background")
     * @return A [GlyphSet] for lazy glyph access, or null if the layer doesn't exist
     */
    fun getGlyphSet(layerName: String): GlyphSet? {
        val layerContents = readLayerContents()
        val entry = layerContents.find { it.first == layerName } ?: return null
        val lib = readLib()

        return GlyphSet(
            layerDirectory = ufo.resolve(entry.second),
            glyphOrder = lib.glyphOrder,
            strict = strict
        )
    }

    /**
     * Returns lazy-loading glyph sets for all layers.
     *
     * @return Map of layer name to [GlyphSet]
     */
    fun getGlyphSets(): Map<String, GlyphSet> {
        val layerContents = readLayerContents()
        val lib = readLib()

        return layerContents.associate { (layerName, directoryName) ->
            layerName to GlyphSet(
                layerDirectory = ufo.resolve(directoryName),
                glyphOrder = lib.glyphOrder,
                strict = strict
            )
        }
    }

    /**
     * Reads a single glyph by name from the default layer.
     *
     * This is a convenience method for loading individual glyphs without
     * creating a [GlyphSet]. For multiple glyph lookups, prefer [getGlyphSet].
     *
     * @param name The glyph name
     * @return The glyph, or null if not found
     */
    fun readGlyph(name: String): GlyphValues? = getGlyphSet()[name]

    /**
     * Reads a single glyph by name from a specific layer.
     *
     * @param name The glyph name
     * @param layerName The layer name
     * @return The glyph, or null if not found or layer doesn't exist
     */
    fun readGlyph(name: String, layerName: String): GlyphValues? = getGlyphSet(layerName)?.get(name)

    fun readGroups(): GroupsValues {
        val path = ufo.resolve("groups.plist")
        val groupsDict = path.readPlist(NSObject::toDictionary, required = false)

        return GroupsValues(groupsDict ?: NSDictionary())
    }

    fun readKerning(): KerningValues {
        val path = ufo.resolve("kerning.plist")
        val kerningDict = path.readPlist(NSObject::toDictionary, required = false)

        return KerningValues(kerningDict ?: NSDictionary())
    }

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

    /**
     * Reads the OpenType feature definitions from features.fea.
     *
     * @return FeaturesValues containing the feature text, or an empty FeaturesValues if the file doesn't exist
     */
    fun readFeatures(): FeaturesValues {
        val path = ufo.resolve("features.fea")
        val text = path.readTextOrNull(required = false)

        return FeaturesValues(text)
    }

    /**
     * Returns an accessor for the images/ directory.
     *
     * The images directory contains PNG images that can be referenced by glyph image elements.
     *
     * @return ImagesDirectory instance for reading/listing images
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
     * @return DataDirectory instance for reading/listing data files
     */
    fun data(): DataDirectory {
        return DataDirectory(ufo.resolve(DataDirectory.DIRECTORY_NAME))
    }

    private fun <T> Path.readPlist(transform: (NSObject) -> T, required: Boolean = true): T? {
        return try {
            transform(XMLPropertyListParser.parse(this))
        } catch (ex: Exception) {
            if (strict && (required || ex !is NoSuchFileException)) {
                throw UFOLibException("Failed to read $name", ex)
            }
            null
        }
    }

    private fun Path.readTextOrNull(required: Boolean = true): String? {
        return try {
            readText()
        } catch (ex: Exception) {
            if (strict && (required || ex !is NoSuchFileException)) {
                throw UFOLibException("Failed to read $name", ex)
            }
            null
        }
    }

    private inline fun <reified T> Path.readXML(): T? {
        return try {
            niceXML.decodeFromString<T>(readText())
        } catch (ex: Exception) {
            if (strict) {
                throw UFOLibException("Failed to read $name", ex)
            }
            null
        }
    }
}

internal fun ufoGlyphOrder(allGlyphs: Collection<String>, glyphOrder: List<String>?): Collection<String> {
    return when {
        glyphOrder.isNullOrEmpty() -> allGlyphs
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
}

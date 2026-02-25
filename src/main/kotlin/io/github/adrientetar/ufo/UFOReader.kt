package io.github.adrientetar.ufo

import com.dd.plist.NSDictionary
import com.dd.plist.NSObject
import com.dd.plist.XMLPropertyListParser
import java.io.Closeable
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText

// TODO: add Log statements

/**
 * UFO font reader, with [ufo] as its path.
 *
 * If [strict] is true, the functions in this class may throw [UFOLibException] if the file doesn't
 * match the file format structure. Otherwise, errors will be silently skipped.
 *
 * Supports UFO format versions 2 and 3. UFO 2 data is transparently converted to UFO 3
 * via [UFO2Converter].
 *
 * For UFOZ (ZIP-compressed UFO) files, use [UFOReader.open] which returns a reader that must
 * be [closed][close] when done to release the ZIP filesystem.
 */
class UFOReader(
    private val ufo: Path,
    private val strict: Boolean = true,
    private val zipFileSystem: FileSystem? = null
) : Closeable {

    companion object {
        /**
         * Opens a UFO or UFOZ file for reading.
         *
         * If [path] is a `.ufoz` file (or any ZIP file), it is opened as a ZIP filesystem
         * and the `.ufo` directory inside is used. The returned reader must be [closed][close]
         * when done to release the ZIP filesystem.
         *
         * If [path] is a directory, a regular [UFOReader] is returned (closing is a no-op).
         */
        fun open(path: Path, strict: Boolean = true): UFOReader {
            if (path.isRegularFile()) {
                val zipFs = FileSystems.newFileSystem(URI.create("jar:${path.toUri()}"), emptyMap<String, Any>())
                try {
                    val root = zipFs.rootDirectories.first()
                    val ufoDir = findUfoDirectory(root)
                        ?: throw UFOLibException("No .ufo directory found in $path")
                    return UFOReader(ufoDir, strict, zipFileSystem = zipFs)
                } catch (ex: Exception) {
                    zipFs.close()
                    throw ex
                }
            }
            return UFOReader(path, strict)
        }

        private fun findUfoDirectory(root: Path): Path? {
            val entries = java.nio.file.Files.list(root).use { it.toList() }
            return entries.firstOrNull { it.isDirectory() && it.name.endsWith(".ufo") }
                ?: entries.firstOrNull { it.isDirectory() }
        }
    }

    /**
     * Closes the underlying ZIP filesystem, if this reader was opened from a UFOZ file.
     * For directory-based readers, this is a no-op.
     */
    override fun close() {
        zipFileSystem?.close()
    }

    /**
     * The UFO format version (2 or 3), detected from metainfo.plist.
     *
     * Defaults to 3 if metainfo.plist is missing or unreadable (e.g. partial UFO).
     */
    val formatVersion: Int by lazy {
        val path = ufo.resolve("metainfo.plist")
        val dict = try {
            XMLPropertyListParser.parse(path).toDictionary()
        } catch (_: Exception) {
            null
        }
        val version = dict?.optInt("formatVersion") ?: 3
        if (version !in 2..3) {
            throw UFOLibException("Unsupported UFO format version: $version")
        }
        version
    }

    fun readFontInfo(): FontInfoValues {
        val path = ufo.resolve("fontinfo.plist")
        val fontInfoDict = path.readPlist(NSObject::toDictionary)

        return FontInfoValues(fontInfoDict ?: NSDictionary())
    }

    /**
     * Reads the layer contents mapping from layercontents.plist.
     *
     * For UFO 2, which has no layercontents.plist, returns a single default layer
     * pointing to the `glyphs` directory.
     *
     * The result is cached after first read.
     *
     * @return List of pairs where each pair is (layerName, directoryName)
     */
    fun readLayerContents(): List<Pair<String, String>> = cachedLayerContents

    private val cachedLayerContents: List<Pair<String, String>> by lazy {
        if (formatVersion < 3) {
            return@lazy listOf(Layer.DEFAULT_NAME to Layer.DEFAULT_DIRECTORY)
        }

        val layerContents = ufo.resolve("layercontents.plist")
            .readPlist(NSObject::toListOfListOfStrings) ?: return@lazy listOf(
                Layer.DEFAULT_NAME to Layer.DEFAULT_DIRECTORY
            )

        layerContents.mapNotNull { entry ->
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
        // Read foreground layer directory from layer contents
        val layerContents = readLayerContents()
        val foregroundDir = (layerContents.firstOrNull()?.second ?: Layer.DEFAULT_DIRECTORY) + "/"

        // Read glyph names according to glyph order
        val contentsDict = ufo.resolve(foregroundDir + "contents.plist")
            .readPlist(NSObject::toMapOfStrings) ?: return sequenceOf()
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
                val glif = glifXml?.let {
                    try {
                        GlifParser.parse(it)
                    } catch (_: Exception) {
                        if (strict) throw UFOLibException("Failed to parse $glifFileName")
                        null
                    }
                }

                if (glif != null) {
                    // Convert GLIF format 1 → 2 (UFO 2 anchor-as-contour → anchor elements)
                    UFO2Converter.convertGlif(glif)
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
        val groups = GroupsValues(groupsDict ?: NSDictionary())

        // For UFO 2, kerning group conversion requires both groups and kerning together.
        // Callers needing converted groups+kerning should use readConvertedGroupsAndKerning().
        return groups
    }

    fun readKerning(): KerningValues {
        val path = ufo.resolve("kerning.plist")
        val kerningDict = path.readPlist(NSObject::toDictionary, required = false)
        val kerning = KerningValues(kerningDict ?: NSDictionary())

        // For UFO 2, kerning group conversion requires both groups and kerning together.
        // Callers needing converted groups+kerning should use readConvertedGroupsAndKerning().
        return kerning
    }

    /**
     * Reads groups and kerning, converting UFO 2 group names to UFO 3 conventions.
     *
     * For UFO 2, `@MMK_L_*` and `@MMK_R_*` group prefixes are renamed to `public.kern1.*`
     * and `public.kern2.*`, and kerning references are updated accordingly.
     *
     * For UFO 3, the values are returned as-is.
     *
     * @return Pair of (groups, kerning) with UFO 3 kerning group conventions
     */
    fun readConvertedGroupsAndKerning(): Pair<GroupsValues, KerningValues> {
        val groups = readGroups()
        val kerning = readKerning()

        if (formatVersion < 3) {
            UFO2Converter.convertKerningAndGroups(kerning, groups)
        }

        return groups to kerning
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
                // Provide clearer error if the entire UFO folder doesn't exist
                if (ex is NoSuchFileException && !ufo.exists()) {
                    throw UFOLibException("File not found: ${ufo.name}", ex)
                }
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
                // Provide clearer error if the entire UFO folder doesn't exist
                if (ex is NoSuchFileException && !ufo.exists()) {
                    throw UFOLibException("File not found: ${ufo.name}", ex)
                }
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

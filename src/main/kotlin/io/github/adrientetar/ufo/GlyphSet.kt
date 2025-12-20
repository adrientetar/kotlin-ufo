package io.github.adrientetar.ufo

import com.dd.plist.NSObject
import com.dd.plist.XMLPropertyListParser
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * A lazy-loading collection of glyphs from a UFO layer directory.
 * This class loads glyphs on-demand, making it suitable for large fonts.
 *
 * Glyphs are cached after first load. Use [clearCache] to free memory, or
 * [setCacheEnabled] to disable caching entirely for memory-constrained scenarios.
 *
 * Example usage:
 * ```kotlin
 * val reader = UFOReader(path)
 * val glyphSet = reader.getGlyphSet()
 *
 * // Load only the glyphs you need
 * val aGlyph = glyphSet["a"]
 * val bGlyph = glyphSet["b"]
 *
 * // Or iterate (still lazy per-glyph)
 * for (glyph in glyphSet) {
 *     println(glyph.name)
 * }
 * ```
 *
 * @property layerDirectory The path to the layer directory (e.g., "glyphs" or "glyphs.background")
 * @property glyphOrder Optional glyph order for iteration ordering
 */
class GlyphSet internal constructor(
    private val layerDirectory: Path,
    private val glyphOrder: List<String>? = null,
    private val strict: Boolean = true
) : Iterable<GlyphValues> {

    // Glyph name -> filename mapping (loaded once from contents.plist)
    private val contentsMap: Map<String, String> by lazy {
        loadContentsMap()
    }

    // Cache for loaded glyphs
    private val cache = ConcurrentHashMap<String, GlyphValues>()

    // Whether caching is enabled
    private var cacheEnabled = true

    /**
     * Returns all glyph names in this set.
     *
     * The order depends on whether a glyph order was provided:
     * - With glyph order: names are returned in that order (only including existing glyphs)
     * - Without glyph order: names are returned in arbitrary order
     *
     * Note: Only glyphs that exist in the contents.plist are included.
     * Glyph order entries for non-existent glyphs are filtered out.
     */
    val glyphNames: List<String> by lazy {
        ufoGlyphOrder(contentsMap.keys, glyphOrder)
            .filter { contentsMap.containsKey(it) }
            .toList()
    }

    /**
     * Returns the number of glyphs in this set.
     */
    val size: Int
        get() = contentsMap.size

    /**
     * Returns true if this set contains no glyphs.
     */
    val isEmpty: Boolean
        get() = contentsMap.isEmpty()

    /**
     * Returns true if a glyph with the given name exists in this set.
     */
    operator fun contains(name: String): Boolean = contentsMap.containsKey(name)

    /**
     * Gets a glyph by name, loading it from disk if not cached.
     *
     * @param name The glyph name
     * @return The glyph, or null if not found
     */
    operator fun get(name: String): GlyphValues? {
        // Check cache first
        cache[name]?.let { return it }

        // Get filename from contents map
        val fileName = contentsMap[name] ?: return null

        // Load from disk
        val glyph = loadGlyph(name, fileName) ?: return null

        // Cache if enabled
        if (cacheEnabled) {
            cache[name] = glyph
        }

        return glyph
    }

    /**
     * Gets a glyph by name, throwing an exception if not found.
     *
     * @param name The glyph name
     * @return The glyph
     * @throws NoSuchElementException if the glyph is not found
     */
    fun getValue(name: String): GlyphValues {
        return get(name) ?: throw NoSuchElementException("Glyph not found: $name")
    }

    /**
     * Gets multiple glyphs by name.
     *
     * @param names The glyph names to load
     * @return List of glyphs (nulls filtered out for missing glyphs)
     */
    fun getAll(names: Collection<String>): List<GlyphValues> {
        return names.mapNotNull { get(it) }
    }

    /**
     * Gets multiple glyphs by name, preserving order and including nulls for missing glyphs.
     *
     * @param names The glyph names to load
     * @return List of glyphs (with nulls for missing glyphs)
     */
    fun getAllOrNull(names: Collection<String>): List<GlyphValues?> {
        return names.map { get(it) }
    }

    /**
     * Preloads glyphs into the cache.
     *
     * Use this to batch-load glyphs you know you'll need, which can be more
     * efficient than loading them one at a time.
     *
     * @param names The glyph names to preload. If null, preloads all glyphs.
     */
    fun preload(names: Collection<String>? = null) {
        val toLoad = names ?: glyphNames
        for (name in toLoad) {
            get(name) // This will cache the glyph
        }
    }

    /**
     * Returns an iterator over all glyphs in glyph order.
     *
     * Glyphs are loaded lazily as the iterator advances. Each glyph is only
     * loaded when [Iterator.next] is called.
     */
    override fun iterator(): Iterator<GlyphValues> = GlyphIterator()

    /**
     * Returns a sequence over all glyphs in glyph order.
     *
     * This is the most memory-efficient way to process all glyphs, as it
     * loads them one at a time and allows for early termination.
     */
    fun asSequence(): Sequence<GlyphValues> = sequence {
        for (name in glyphNames) {
            get(name)?.let { yield(it) }
        }
    }

    /**
     * Clears the glyph cache, freeing memory.
     *
     * Glyphs will be reloaded from disk on next access.
     */
    fun clearCache() {
        cache.clear()
    }

    /**
     * Returns the number of glyphs currently cached.
     */
    val cacheSize: Int
        get() = cache.size

    /**
     * Enables or disables glyph caching.
     *
     * When disabled, glyphs are loaded fresh from disk on each access.
     * This uses less memory but is slower for repeated access.
     *
     * @param enabled Whether to enable caching
     */
    fun setCacheEnabled(enabled: Boolean) {
        cacheEnabled = enabled
        if (!enabled) {
            clearCache()
        }
    }

    /**
     * Returns true if caching is enabled.
     */
    fun isCacheEnabled(): Boolean = cacheEnabled

    // Private implementation

    private fun loadContentsMap(): Map<String, String> {
        val contentsPath = layerDirectory.resolve("contents.plist")
        return try {
            val nsObject = XMLPropertyListParser.parse(contentsPath)
            nsObject.toMapOfStrings()
        } catch (ex: NoSuchFileException) {
            emptyMap()
        } catch (ex: Exception) {
            if (strict) {
                throw UFOLibException("Failed to read ${contentsPath.name}", ex)
            }
            emptyMap()
        }
    }

    private fun loadGlyph(name: String, fileName: String): GlyphValues? {
        val glifPath = layerDirectory.resolve(fileName)
        val glifXml = try {
            glifPath.readText()
        } catch (ex: Exception) {
            if (strict) {
                throw UFOLibException("Failed to read glyph: $name", ex)
            }
            return null
        }

        val glif = try {
            GlifParser.parse(glifXml)
        } catch (ex: Exception) {
            if (strict) {
                throw UFOLibException("Failed to parse glyph: $name", ex)
            }
            return null
        }

        return GlyphValues(glif)
    }

    private inner class GlyphIterator : Iterator<GlyphValues> {
        private val nameIterator = glyphNames.iterator()
        private var nextGlyph: GlyphValues? = null

        init {
            advance()
        }

        override fun hasNext(): Boolean = nextGlyph != null

        override fun next(): GlyphValues {
            val result = nextGlyph ?: throw NoSuchElementException()
            advance()
            return result
        }

        private fun advance() {
            nextGlyph = null
            while (nameIterator.hasNext() && nextGlyph == null) {
                nextGlyph = get(nameIterator.next())
            }
        }
    }
}

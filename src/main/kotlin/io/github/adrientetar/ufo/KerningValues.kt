package io.github.adrientetar.ufo

import com.dd.plist.NSDictionary
import com.dd.plist.NSNumber
import com.dd.plist.NSObject

/**
 * Represents the contents of the `kerning.plist` file.
 *
 * Kerning is stored as a nested dictionary where:
 * - The first key is the first glyph/group name (left side in LTR)
 * - The second key is the second glyph/group name (right side in LTR)
 * - The value is the kerning adjustment in font units
 *
 * Group names use special prefixes:
 * - `public.kern1.` for first/left side kerning groups
 * - `public.kern2.` for second/right side kerning groups
 *
 * See: https://unifiedfontobject.org/versions/ufo3/kerning.plist/
 */
class KerningValues(@PublishedApi internal val dict: NSDictionary = NSDictionary()) {

    /**
     * Returns the set of all first (left) side glyph/group names.
     */
    val firstGlyphs: Set<String>
        get() = dict.allKeys().toSet()

    /**
     * Returns true if kerning exists for the given first glyph/group.
     */
    fun containsFirst(first: String): Boolean {
        return dict.containsKey(first)
    }

    /**
     * Gets all kerning pairs for a given first (left) glyph/group.
     *
     * @param first The first glyph or group name
     * @return Map of second glyph/group names to kerning values, or null if no pairs exist
     */
    operator fun get(first: String): Map<String, Float>? {
        val secondDict = dict[first] as? NSDictionary ?: return null
        return secondDict.allKeys().associateWith { second ->
            (secondDict[second] as? NSNumber)?.floatValue() ?: 0f
        }
    }

    /**
     * Gets a specific kerning value.
     *
     * @param first The first glyph or group name
     * @param second The second glyph or group name
     * @return The kerning value, or null if not defined
     */
    fun get(first: String, second: String): Float? {
        val secondDict = dict[first] as? NSDictionary ?: return null
        return (secondDict[second] as? NSNumber)?.floatValue()
    }

    /**
     * Sets all kerning pairs for a given first (left) glyph/group.
     *
     * @param first The first glyph or group name
     * @param pairs Map of second glyph/group names to kerning values, or null to remove all pairs
     */
    operator fun set(first: String, pairs: Map<String, Float>?) {
        if (pairs == null || pairs.isEmpty()) {
            dict.remove(first)
        } else {
            val secondDict = NSDictionary()
            pairs.forEach { (second, value) ->
                secondDict.put(second, value)
            }
            dict.put(first, secondDict)
        }
    }

    /**
     * Sets a specific kerning value.
     *
     * @param first The first glyph or group name
     * @param second The second glyph or group name
     * @param value The kerning value, or null to remove the pair
     */
    fun set(first: String, second: String, value: Float?) {
        if (value == null) {
            val secondDict = dict[first] as? NSDictionary ?: return
            secondDict.remove(second)
            if (secondDict.count() == 0) {
                dict.remove(first)
            }
        } else {
            val secondDict = (dict[first] as? NSDictionary) ?: NSDictionary().also {
                dict.put(first, it)
            }
            secondDict.put(second, value)
        }
    }

    /**
     * Removes all kerning pairs for a given first glyph/group.
     *
     * @param first The first glyph or group name to remove
     */
    fun remove(first: String) {
        dict.remove(first)
    }

    /**
     * Removes a specific kerning pair.
     *
     * @param first The first glyph or group name
     * @param second The second glyph or group name
     */
    fun remove(first: String, second: String) {
        set(first, second, null)
    }

    /**
     * Iterates over all kerning pairs without allocating collections.
     * More efficient than [toMap] or [toList] when you only need to iterate once.
     */
    inline fun forEach(action: (first: String, second: String, value: Float) -> Unit) {
        for (first in dict.allKeys()) {
            val secondDict = dict[first] as? NSDictionary ?: continue
            for (second in secondDict.allKeys()) {
                val value = (secondDict[second] as? NSNumber)?.floatValue() ?: continue
                action(first, second, value)
            }
        }
    }

    /**
     * Iterates over first-level entries (first glyph -> pairs map).
     */
    inline fun forEachFirst(action: (first: String, pairs: Map<String, Float>) -> Unit) {
        for (first in dict.allKeys()) {
            val pairs = this[first] ?: continue
            action(first, pairs)
        }
    }

    /**
     * Returns the total number of kerning pairs.
     *
     * Note: This iterates through all first-level entries to count pairs.
     * For large kerning tables, consider caching this value if needed repeatedly.
     */
    val pairCount: Int
        get() = dict.allKeys().sumOf { first ->
            (dict[first] as? NSDictionary)?.count() ?: 0
        }
}

/**
 * Extension function to create a KerningValues from a nested map.
 */
fun Map<String, Map<String, Float>>.toKerningValues(): KerningValues {
    val kerning = KerningValues()
    forEach { (first, pairs) ->
        kerning[first] = pairs
    }
    return kerning
}

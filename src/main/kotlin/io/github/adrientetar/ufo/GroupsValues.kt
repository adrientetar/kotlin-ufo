package io.github.adrientetar.ufo

import com.dd.plist.NSArray
import com.dd.plist.NSDictionary
import com.dd.plist.NSString

/**
 * Represents the contents of the `groups.plist` file.
 *
 * Groups are used to define collections of glyphs, commonly for kerning.
 * Kerning groups use special prefixes:
 * - `public.kern1.` for first/left side kerning groups
 * - `public.kern2.` for second/right side kerning groups
 *
 * See: https://unifiedfontobject.org/versions/ufo3/groups.plist/
 */
class GroupsValues(@PublishedApi internal val dict: NSDictionary = NSDictionary()) {

    /**
     * Returns the set of all group names.
     */
    val groupNames: Set<String>
        get() = dict.allKeys().toSet()

    /**
     * Returns true if the group with the given name exists.
     */
    fun containsGroup(name: String): Boolean {
        return dict.containsKey(name)
    }

    /**
     * Gets the list of glyph names in the specified group.
     *
     * @param name The group name
     * @return The list of glyph names, or null if the group doesn't exist
     */
    operator fun get(name: String): List<String>? {
        return (dict[name] as? NSArray)?.array?.mapNotNull { (it as? NSString)?.content }
    }

    /**
     * Sets the list of glyph names for a group.
     *
     * @param name The group name
     * @param glyphs The list of glyph names, or null to remove the group
     */
    operator fun set(name: String, glyphs: List<String>?) {
        if (glyphs == null) {
            dict.remove(name)
        } else {
            dict.put(name, glyphs.toTypedArray())
        }
    }

    /**
     * Removes a group.
     *
     * @param name The group name to remove
     */
    fun remove(name: String) {
        dict.remove(name)
    }

    /**
     * Iterates over all groups without allocating a Map.
     * More efficient than [toMap] when you only need to iterate once.
     */
    inline fun forEach(action: (name: String, glyphs: List<String>) -> Unit) {
        for (key in dict.allKeys()) {
            action(key, this[key] ?: emptyList())
        }
    }

    /**
     * Returns an iterator over all group entries.
     */
    operator fun iterator(): Iterator<Map.Entry<String, List<String>>> = iterator {
        for (key in dict.allKeys()) {
            yield(object : Map.Entry<String, List<String>> {
                override val key: String = key
                override val value: List<String> = this@GroupsValues[key] ?: emptyList()
            })
        }
    }

    /**
     * Iterates over all first-side kerning groups (those with prefix `public.kern1.`).
     */
    inline fun forEachFirstKerningGroup(action: (name: String, glyphs: List<String>) -> Unit) {
        forEach { name, glyphs ->
            if (name.startsWith(PUBLIC_KERN1_PREFIX)) {
                action(name, glyphs)
            }
        }
    }

    /**
     * Iterates over all second-side kerning groups (those with prefix `public.kern2.`).
     */
    inline fun forEachSecondKerningGroup(action: (name: String, glyphs: List<String>) -> Unit) {
        forEach { name, glyphs ->
            if (name.startsWith(PUBLIC_KERN2_PREFIX)) {
                action(name, glyphs)
            }
        }
    }

    companion object {
        const val PUBLIC_KERN1_PREFIX = "public.kern1."
        const val PUBLIC_KERN2_PREFIX = "public.kern2."
    }
}

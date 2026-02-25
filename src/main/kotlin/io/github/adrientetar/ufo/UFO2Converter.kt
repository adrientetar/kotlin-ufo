package io.github.adrientetar.ufo

import com.dd.plist.NSDictionary

/**
 * Stateless utility for converting UFO 2 data structures to UFO 3.
 *
 * All methods are pure functions that transform their inputs in-place or return new values.
 * This object can be tested independently of [UFOReader].
 */
object UFO2Converter {

    /**
     * Converts a GLIF format 1 to format 2.
     *
     * In GLIF 1, anchors are represented as single-point contours with `type="move"` and an
     * optional `name`. This method extracts those contours as [Anchor] objects, removes them
     * from the outline, and sets the format to 2.
     *
     * See: https://unifiedfontobject.org/versions/ufo3/glyphs/glif/#converting-implied-anchors-in-glif-1-to-glif-2-anchor-elements
     */
    fun convertGlif(glif: Glif) {
        if (glif.format != 1) return

        val contours = glif.outline.contours ?: run {
            glif.format = 2
            return
        }

        val anchors = mutableListOf<Anchor>()
        val remainingContours = mutableListOf<Contour>()

        for (contour in contours) {
            val anchor = extractAnchorFromContour(contour)
            if (anchor != null) {
                anchors.add(anchor)
            } else {
                remainingContours.add(contour)
            }
        }

        glif.outline.contours = remainingContours.ifEmpty { null }

        if (anchors.isNotEmpty()) {
            val existing = glif.anchors.orEmpty()
            glif.anchors = existing + anchors
        }

        glif.format = 2
    }

    /**
     * Converts UFO 2 kerning group names to UFO 3 conventions.
     *
     * Renames `@MMK_L_*` → `public.kern1.*` and `@MMK_R_*` → `public.kern2.*` in both
     * the groups dictionary and the kerning dictionary references.
     *
     * Following the algorithm from fonttools' `converters.py`.
     */
    fun convertKerningAndGroups(
        kerning: KerningValues,
        groups: GroupsValues
    ) {
        val renameMap = buildGroupRenameMap(groups, kerning)
        if (renameMap.isEmpty()) return

        // Add renamed groups to the groups dict
        for ((oldName, newName) in renameMap) {
            val members = groups.dict[oldName] ?: continue
            groups.dict.put(newName, members)
        }

        // Rename references in the kerning dict
        val kerningDict = kerning.dict
        val firstKeys = kerningDict.allKeys().toList()
        for (first in firstKeys) {
            val seconds = kerningDict[first]?.toDictionary() ?: continue
            val newFirst = renameMap[first] ?: first

            val newSeconds = NSDictionary()
            for (second in seconds.allKeys()) {
                val value = seconds[second] ?: continue
                val newSecond = renameMap[second] ?: second
                newSeconds.put(newSecond, value)
            }

            if (newFirst != first) {
                kerningDict.remove(first)
            }
            kerningDict.put(newFirst, newSeconds)
        }
    }

    /**
     * Builds a map of old group names → new group names for kerning groups.
     *
     * Scans for `@MMK_L_` and `@MMK_R_` prefixed groups, as well as non-prefixed groups
     * that are referenced in kerning pairs.
     */
    private fun buildGroupRenameMap(
        groups: GroupsValues,
        kerning: KerningValues
    ): Map<String, String> {
        val firstGroups = mutableSetOf<String>()
        val secondGroups = mutableSetOf<String>()

        // Find groups with known MMK prefixes
        for (groupName in groups.dict.allKeys()) {
            when {
                groupName.startsWith("@MMK_L_") -> firstGroups.add(groupName)
                groupName.startsWith("@MMK_R_") -> secondGroups.add(groupName)
            }
        }

        // Find non-prefixed groups referenced in kerning
        val groupNames = groups.dict.allKeys().toSet()
        val kerningDict = kerning.dict
        for (first in kerningDict.allKeys()) {
            if (first in groupNames && !first.startsWith("public.kern1.")) {
                firstGroups.add(first)
            }
            val seconds = kerningDict[first]?.toDictionary() ?: continue
            for (second in seconds.allKeys()) {
                if (second in groupNames && !second.startsWith("public.kern2.")) {
                    secondGroups.add(second)
                }
            }
        }

        val renameMap = mutableMapOf<String, String>()
        for (name in firstGroups) {
            val stripped = name.removePrefix("@MMK_L_")
            renameMap[name] = "public.kern1.$stripped"
        }
        for (name in secondGroups) {
            val stripped = name.removePrefix("@MMK_R_")
            renameMap[name] = "public.kern2.$stripped"
        }

        return renameMap
    }

    private fun extractAnchorFromContour(contour: Contour): Anchor? {
        if (contour.points.size != 1) return null
        val point = contour.points[0]
        if (point.type != "move") return null

        return Anchor(
            x = point.x,
            y = point.y,
            name = point.name
        )
    }
}

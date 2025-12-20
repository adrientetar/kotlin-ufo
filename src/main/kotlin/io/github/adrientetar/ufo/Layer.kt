package io.github.adrientetar.ufo

import com.dd.plist.NSDictionary

/**
 * Represents a glyph layer in a UFO font.
 *
 * A layer contains a set of glyphs and optional metadata (color, lib).
 * The default layer is typically named "public.default" or has a custom name,
 * and is stored in the "glyphs" directory. Additional layers are stored in
 * directories like "glyphs.layername".
 *
 * @property name The user-visible layer name (e.g., "public.default", "public.background", "Sketches")
 * @property directoryName The directory name where glyphs are stored (e.g., "glyphs", "glyphs.S_ketches")
 * @property glyphs The glyphs in this layer
 * @property info Optional layer metadata (color, lib)
 */
data class Layer(
    val name: String,
    val directoryName: String = DEFAULT_DIRECTORY,
    val glyphs: MutableList<GlyphValues> = mutableListOf(),
    var info: LayerInfo? = null
) {
    /**
     * Returns true if this is the default layer.
     */
    val isDefault: Boolean
        get() = directoryName == DEFAULT_DIRECTORY

    /**
     * Finds a glyph by name in this layer.
     */
    fun getGlyph(name: String): GlyphValues? = glyphs.find { it.name == name }

    /**
     * Returns the names of all glyphs in this layer.
     */
    fun glyphNames(): List<String> = glyphs.mapNotNull { it.name }

    companion object {
        /** The default layer name when no custom name is provided. */
        const val DEFAULT_NAME = "public.default"

        /** The default directory name for the primary glyph layer. */
        const val DEFAULT_DIRECTORY = "glyphs"

        /** The standard name for background/reference layers. */
        const val BACKGROUND_NAME = "public.background"

        /**
         * Creates a default foreground layer.
         */
        fun createDefault(glyphs: List<GlyphValues> = emptyList()): Layer {
            return Layer(
                name = DEFAULT_NAME,
                directoryName = DEFAULT_DIRECTORY,
                glyphs = glyphs.toMutableList()
            )
        }

        /**
         * Creates a background layer.
         */
        fun createBackground(glyphs: List<GlyphValues> = emptyList()): Layer {
            return Layer(
                name = BACKGROUND_NAME,
                directoryName = "glyphs.public.background",
                glyphs = glyphs.toMutableList()
            )
        }
    }
}

/**
 * Layer metadata from layerinfo.plist.
 *
 * @property color The color that should be used for all glyphs in the layer.
 *                 Format follows the UFO color definition standard (comma-separated RGBA values 0-1).
 * @property lib A dictionary of arbitrary data specific to this layer.
 */
data class LayerInfo(
    var color: String? = null,
    var lib: NSDictionary? = null
) {
    /**
     * Returns true if this layer info has any data.
     */
    val isEmpty: Boolean
        get() = color == null && (lib == null || lib!!.count() == 0)

    companion object {
        internal fun fromDictionary(dict: NSDictionary): LayerInfo {
            return LayerInfo(
                color = dict.optString("color"),
                lib = dict["lib"] as? NSDictionary
            )
        }

        internal fun toDictionary(info: LayerInfo): NSDictionary {
            val dict = NSDictionary()
            info.color?.let { dict.put("color", it) }
            info.lib?.let { dict.put("lib", it) }
            return dict
        }
    }
}

/**
 * Converts a layer name to a valid directory name.
 *
 * The directory name will be "glyphs." followed by a sanitized version of the layer name.
 * Uses the same filename conversion algorithm as glyph names.
 */
fun layerNameToDirectoryName(layerName: String): String {
    if (layerName == Layer.DEFAULT_NAME) {
        return Layer.DEFAULT_DIRECTORY
    }
    return "glyphs.${layerName.toFileName()}"
}

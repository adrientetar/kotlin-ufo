package io.github.adrientetar.ufo

import java.io.Closeable

/**
 * Common interface for writing UFO font data.
 *
 * Implemented by [UFOWriter] (directory-based UFO) and [UFOZWriter] (ZIP-compressed UFOZ),
 * allowing callers to write to either format without branching on type.
 */
interface UFOFormatWriter : Closeable {
    fun writeMetaInfo()
    fun writeFontInfo(values: FontInfoValues)
    fun writeLayers(layers: List<Layer>)
    fun writeLayerGlyphs(layer: Layer)
    fun writeGlyphs(glyphs: List<GlyphValues>)
    fun writeGroups(values: GroupsValues)
    fun writeKerning(values: KerningValues)
    fun writeLib(values: LibValues)
    fun writeFeatures(values: FeaturesValues)
    fun images(): ImagesDirectory
    fun data(): DataDirectory
}

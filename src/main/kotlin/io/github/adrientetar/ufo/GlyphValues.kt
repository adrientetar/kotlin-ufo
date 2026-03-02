package io.github.adrientetar.ufo

import com.dd.plist.NSDictionary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

class GlyphValues(internal val glif: Glif = Glif()) {
    var anchors: List<Anchor>?
        get() = glif.anchors
        set(value) { glif.anchors = value }

    var components: List<Component>?
        get() = glif.outline.components
        set(value) { glif.outline.components = value }

    var contours: List<Contour>?
        get() = glif.outline.contours
        set(value) { glif.outline.contours = value }

    /**
     * Per-glyph guidelines.
     */
    var guidelines: List<GlyphGuideline>?
        get() = glif.guidelines
        set(value) { glif.guidelines = value }

    var height: Float?
        get() = glif.advance.height
        set(value) { glif.advance.height = value }

    /**
     * Image reference for this glyph.
     *
     * The image file must be in PNG format and stored in the images/ directory.
     */
    var image: Image?
        get() = glif.image
        set(value) { glif.image = value }

    /**
     * The glyph's lib data for storing arbitrary custom data.
     */
    val lib: GlyphLib
        get() {
            if (glif.lib == null) {
                glif.lib = GlifLib()
            }
            return GlyphLib(glif.lib!!.content)
        }

    var name: String?
        get() = glif.name
        set(value) { glif.name = value }

    /**
     * Arbitrary text note about the glyph.
     */
    var note: String?
        get() = glif.note
        set(value) { glif.note = value }

    var unicodes: List<Int>?
        get() = glif.unicodes?.map {
            it.hex.toLong(16).toInt()
        }
        set(value) {
            glif.unicodes = value?.map {
                Unicode("%X".format(it))
            }
        }

    var width: Float?
        get() = glif.advance.width
        set(value) { glif.advance.width = value }

    override fun toString(): String = "GlyphValues(name=$name)"
}

// TODO: it seems like to preserve the order of contour/components, we need to manual parse
//  see https://github.com/pdvrieze/xmlutil/issues/137
@Serializable
@SerialName("glyph")
data class Glif(
    var name: String? = null,
    var format: Int = 2,

    @XmlElement(true)
    val advance: Advance = Advance(),
    @XmlElement(true)
    @XmlSerialName("unicode", "", "")
    var unicodes: List<Unicode>? = null,
    @XmlElement(true)
    var note: String? = null,
    @XmlElement(true)
    var image: Image? = null,
    @XmlElement(true)
    @XmlSerialName("guideline", "", "")
    var guidelines: List<GlyphGuideline>? = null,
    @XmlElement(true)
    @XmlSerialName("anchor", "", "")
    var anchors: List<Anchor>? = null,
    @XmlElement(true)
    val outline: Outline = Outline(),
) {
    // lib is handled separately due to plist-in-XML format
    @kotlinx.serialization.Transient
    var lib: GlifLib? = null
}

@Serializable
@SerialName("advance")
data class Advance(
    var height: Float? = null,
    var width: Float? = null
)

@Serializable
@SerialName("unicode")
data class Unicode(
    val hex: String
)

@Serializable
@SerialName("outline")
data class Outline(
    @XmlElement(true)
    @XmlSerialName("component", "", "")
    var components: List<Component>? = null,

    @XmlElement(true)
    @XmlSerialName("contour", "", "")
    var contours: List<Contour>? = null
)

@Serializable
@SerialName("component")
data class Component(
    val base: String,
    val xScale: Float? = null,
    val xyScale: Float? = null,
    val yxScale: Float? = null,
    val yScale: Float? = null,
    val xOffset: Float? = null,
    val yOffset: Float? = null,
    val identifier: String? = null
)

@Serializable
@SerialName("contour")
data class Contour(
    val identifier: String? = null,
    @XmlElement(true)
    @XmlSerialName("point", "", "")
    val points: List<Point> = emptyList()
)

@Serializable
@SerialName("point")
data class Point(
    val x: Float,
    val y: Float,
    val type: String? = null,
    val smooth: String? = null,
    val name: String? = null,
    val identifier: String? = null
)

@Serializable
@SerialName("anchor")
data class Anchor(
    val x: Float,
    val y: Float,
    val name: String? = null,
    val color: String? = null,
    val identifier: String? = null
)

/**
 * A reference guideline for a glyph.
 *
 * The guideline extends along [angle] to infinity in both directions out of the point
 * defined by [x] and [y]. If [y] and [angle] are omitted, the element represents a
 * vertical guideline. If [x] and [angle] are omitted, the element represents a
 * horizontal guideline.
 */
@Serializable
@SerialName("guideline")
data class GlyphGuideline(
    val x: Float? = null,
    val y: Float? = null,
    val angle: Float? = null,
    val name: String? = null,
    val color: String? = null,
    val identifier: String? = null
)

/**
 * An image reference.
 *
 * The transformation matrix is formed by [xScale], [xyScale], [yxScale], [yScale],
 * [xOffset], [yOffset] in that order. The default is the identity matrix [1 0 0 1 0 0].
 *
 * One image width unit equals one horizontal font unit and one image height unit
 * equals one vertical font unit before the transformation is applied.
 */
@Serializable
@SerialName("image")
data class Image(
    val fileName: String,
    val xScale: Float? = null,
    val xyScale: Float? = null,
    val yxScale: Float? = null,
    val yScale: Float? = null,
    val xOffset: Float? = null,
    val yOffset: Float? = null,
    val color: String? = null
)

fun contourOf(vararg points: Point) = Contour(points = points.toList())

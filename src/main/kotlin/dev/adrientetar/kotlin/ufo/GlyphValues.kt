package dev.adrientetar.kotlin.ufo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

// TODO: make internal and extract an interface for the public API
class GlyphValues(internal val glif: Glif = Glif()) {
    val anchors: List<Anchor>?
        get() = glif.anchors

    var components: List<Component>?
        get() = glif.outline.components
        set(value) { glif.outline.components = value }

    var contours: List<Contour>?
        get() = glif.outline.contours
        set(value) { glif.outline.contours = value }

    var height: Float?
        get() = glif.advance.height
        set(value) { glif.advance.height = value }

    var name: String?
        get() = glif.name
        set(value) { glif.name = value }

    val unicodes: List<Int>?
        get() = glif.unicodes?.map {
            it.hex.toLong(16).toInt()
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
    @XmlSerialName("anchor", "", "")
    var anchors: List<Anchor>? = null,
    @XmlElement(true)
    val outline: Outline = Outline()
)

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
    val xScale: Float?,
    val xyScale: Float?,
    val yxScale: Float?,
    val yScale: Float?,
    val xOffset: Float?,
    val yOffset: Float?
)

@Serializable
@SerialName("contour")
data class Contour(
    @XmlElement(true)
    @XmlSerialName("point", "", "")
    val points: List<Point>
)

@Serializable
@SerialName("point")
data class Point(
    val x: Float,
    val y: Float,
    val type: String? = null,
    val smooth: String? = null
)

@Serializable
@SerialName("anchor")
data class Anchor(
    val x: Float,
    val y: Float,
    val name: String?
)

fun contourOf(vararg points: Point) = Contour(points.toList())

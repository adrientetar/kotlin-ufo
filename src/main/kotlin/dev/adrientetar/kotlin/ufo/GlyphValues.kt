package dev.adrientetar.kotlin.ufo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

// TODO: make internal and extract an interface for the public API
class GlyphValues(private val glif: Glif) {
    val anchors: List<Anchor>
        get() = glif.anchors

    val components: List<Component>
        get() = glif.outline?.components ?: emptyList()

    val contours: List<Contour>
        get() = glif.outline?.contours ?: emptyList()

    val name: String
        get() = glif.name

    val unicodes: List<Int>
        get() = glif.unicodes.map {
            it.hex.toLong(16).toInt()
        }

    val width: Float
        get() = glif.advance?.width ?: 0f
}

// TODO: it seems like to preserve the order of contour/components, we need to manual parse
//  see https://github.com/pdvrieze/xmlutil/issues/137
@Serializable
@SerialName("glyph")
data class Glif(
    val name: String,
    val format: Int,

    @XmlElement(true)
    val advance: Advance?,
    @XmlElement(true)
    @XmlSerialName("unicode", "", "")
    val unicodes: List<Unicode>,
    @XmlElement(true)
    @XmlSerialName("anchor", "", "")
    val anchors: List<Anchor>,
    @XmlElement(true)
    val outline: Outline?
)

@Serializable
@SerialName("advance")
data class Advance(
    val width: Float
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
    val components: List<Component>,

    @XmlElement(true)
    @XmlSerialName("contour", "", "")
    val contours: List<Contour>
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

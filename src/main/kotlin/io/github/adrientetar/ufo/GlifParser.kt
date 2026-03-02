package io.github.adrientetar.ufo

import java.io.StringReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader

/**
 * A fast streaming parser for GLIF (Glyph Interchange Format) files.
 */
object GlifParser {

    private val xmlInputFactory: XMLInputFactory by lazy {
        XMLInputFactory.newInstance().apply {
            // Disable external entities for security
            setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
            setProperty(XMLInputFactory.SUPPORT_DTD, false)
            // Enable coalescing for text content
            setProperty(XMLInputFactory.IS_COALESCING, true)
        }
    }

    /**
     * Parses a GLIF XML string into a Glif object.
     *
     * @param xml The GLIF XML content
     * @return The parsed Glif object
     * @throws GlifParseException if the XML is malformed or invalid
     */
    fun parse(xml: String): Glif {
        val reader = xmlInputFactory.createXMLStreamReader(StringReader(xml))
        try {
            return parseGlif(reader, xml)
        } finally {
            reader.close()
        }
    }

    private fun parseGlif(reader: XMLStreamReader, originalXml: String): Glif {
        val glif = Glif()
        val unicodes = mutableListOf<Unicode>()
        val guidelines = mutableListOf<GlyphGuideline>()
        val anchors = mutableListOf<Anchor>()
        val outlineElements = mutableListOf<OutlineElement>()

        while (reader.hasNext()) {
            when (reader.next()) {
                XMLStreamConstants.START_ELEMENT -> {
                    when (reader.localName) {
                        "glyph" -> {
                            glif.name = reader.getAttributeValue(null, "name")
                            reader.getAttributeValue(null, "format")?.toIntOrNull()?.let {
                                glif.format = it
                            }
                        }
                        "advance" -> {
                            reader.getAttributeValue(null, "width")?.toFloatOrNull()?.let {
                                glif.advance.width = it
                            }
                            reader.getAttributeValue(null, "height")?.toFloatOrNull()?.let {
                                glif.advance.height = it
                            }
                        }
                        "unicode" -> {
                            reader.getAttributeValue(null, "hex")?.let {
                                unicodes.add(Unicode(it))
                            }
                        }
                        "note" -> {
                            glif.note = reader.elementText
                        }
                        "image" -> {
                            glif.image = parseImage(reader)
                        }
                        "guideline" -> {
                            guidelines.add(parseGuideline(reader))
                        }
                        "anchor" -> {
                            anchors.add(parseAnchor(reader))
                        }
                        "outline" -> {
                            parseOutline(reader, outlineElements)
                        }
                        "lib" -> {
                            // For lib, we extract and parse the plist content
                            glif.lib = parseLib(originalXml)
                        }
                    }
                }
            }
        }

        if (unicodes.isNotEmpty()) glif.unicodes = unicodes
        if (guidelines.isNotEmpty()) glif.guidelines = guidelines
        if (anchors.isNotEmpty()) glif.anchors = anchors
        glif.outline.elements.addAll(outlineElements)

        return glif
    }

    private fun parseImage(reader: XMLStreamReader): Image {
        return Image(
            fileName = reader.getAttributeValue(null, "fileName") ?: "",
            xScale = reader.getAttributeValue(null, "xScale")?.toFloatOrNull(),
            xyScale = reader.getAttributeValue(null, "xyScale")?.toFloatOrNull(),
            yxScale = reader.getAttributeValue(null, "yxScale")?.toFloatOrNull(),
            yScale = reader.getAttributeValue(null, "yScale")?.toFloatOrNull(),
            xOffset = reader.getAttributeValue(null, "xOffset")?.toFloatOrNull(),
            yOffset = reader.getAttributeValue(null, "yOffset")?.toFloatOrNull(),
            color = reader.getAttributeValue(null, "color")
        )
    }

    private fun parseGuideline(reader: XMLStreamReader): GlyphGuideline {
        return GlyphGuideline(
            x = reader.getAttributeValue(null, "x")?.toFloatOrNull(),
            y = reader.getAttributeValue(null, "y")?.toFloatOrNull(),
            angle = reader.getAttributeValue(null, "angle")?.toFloatOrNull(),
            name = reader.getAttributeValue(null, "name"),
            color = reader.getAttributeValue(null, "color"),
            identifier = reader.getAttributeValue(null, "identifier")
        )
    }

    private fun parseAnchor(reader: XMLStreamReader): Anchor {
        return Anchor(
            x = reader.getAttributeValue(null, "x")?.toFloatOrNull() ?: 0f,
            y = reader.getAttributeValue(null, "y")?.toFloatOrNull() ?: 0f,
            name = reader.getAttributeValue(null, "name"),
            color = reader.getAttributeValue(null, "color"),
            identifier = reader.getAttributeValue(null, "identifier")
        )
    }

    private fun parseOutline(
        reader: XMLStreamReader,
        elements: MutableList<OutlineElement>
    ) {
        var depth = 1
        var currentContourIdentifier: String? = null
        var currentPoints = mutableListOf<Point>()

        while (reader.hasNext() && depth > 0) {
            when (reader.next()) {
                XMLStreamConstants.START_ELEMENT -> {
                    depth++
                    when (reader.localName) {
                        "component" -> {
                            elements.add(parseComponent(reader))
                        }
                        "contour" -> {
                            currentContourIdentifier = reader.getAttributeValue(null, "identifier")
                            currentPoints = mutableListOf()
                        }
                        "point" -> {
                            currentPoints.add(parsePoint(reader))
                        }
                    }
                }
                XMLStreamConstants.END_ELEMENT -> {
                    depth--
                    if (reader.localName == "contour" && currentPoints.isNotEmpty()) {
                        elements.add(Contour(
                            identifier = currentContourIdentifier,
                            points = currentPoints.toList()
                        ))
                    }
                }
            }
        }
    }

    private fun parseComponent(reader: XMLStreamReader): Component {
        return Component(
            base = reader.getAttributeValue(null, "base") ?: "",
            xScale = reader.getAttributeValue(null, "xScale")?.toFloatOrNull(),
            xyScale = reader.getAttributeValue(null, "xyScale")?.toFloatOrNull(),
            yxScale = reader.getAttributeValue(null, "yxScale")?.toFloatOrNull(),
            yScale = reader.getAttributeValue(null, "yScale")?.toFloatOrNull(),
            xOffset = reader.getAttributeValue(null, "xOffset")?.toFloatOrNull(),
            yOffset = reader.getAttributeValue(null, "yOffset")?.toFloatOrNull(),
            identifier = reader.getAttributeValue(null, "identifier")
        )
    }

    private fun parsePoint(reader: XMLStreamReader): Point {
        return Point(
            x = reader.getAttributeValue(null, "x")?.toFloatOrNull() ?: 0f,
            y = reader.getAttributeValue(null, "y")?.toFloatOrNull() ?: 0f,
            type = reader.getAttributeValue(null, "type"),
            smooth = reader.getAttributeValue(null, "smooth"),
            name = reader.getAttributeValue(null, "name"),
            identifier = reader.getAttributeValue(null, "identifier")
        )
    }

    /**
     * Parses the lib element from the original XML.
     * We use regex extraction + plist parsing since the lib contains embedded plist XML.
     */
    private fun parseLib(originalXml: String): GlifLib? {
        val libStart = originalXml.indexOf("<lib>")
        if (libStart == -1) return null

        val libEnd = originalXml.indexOf("</lib>", libStart)
        if (libEnd == -1) return null

        val dictStart = originalXml.indexOf("<dict", libStart)
        val dictEnd = originalXml.lastIndexOf("</dict>", libEnd)

        if (dictStart == -1 || dictEnd == -1 || dictStart >= dictEnd) {
            return GlifLib()
        }

        val dictContent = originalXml.substring(dictStart, dictEnd + "</dict>".length)
        val dict = parseDictFromXml(dictContent) ?: return GlifLib()
        return GlifLib(dict)
    }
}

/**
 * Exception thrown when GLIF parsing fails.
 */
class GlifParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

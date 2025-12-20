package io.github.adrientetar.ufo

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import kotlin.test.Test

class IdentifierTests {

    @Test
    fun testReadContourIdentifier() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphs = reader.readGlyphs().toList()

        val glyphC = glyphs.find { it.name == "c" }
        assertThat(glyphC).isNotNull()

        val contours = glyphC!!.contours
        assertThat(contours).isNotNull()
        assertThat(contours).hasSize(1)
        assertThat(contours!![0].identifier).isEqualTo("contour001")
    }

    @Test
    fun testReadPointIdentifierAndName() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphs = reader.readGlyphs().toList()

        val glyphC = glyphs.find { it.name == "c" }
        assertThat(glyphC).isNotNull()

        val points = glyphC!!.contours!![0].points

        // First point has identifier
        val point1 = points[0]
        assertThat(point1.identifier).isEqualTo("point001")
        assertThat(point1.name).isNull()

        // Fourth point has both name and identifier
        val point4 = points[3]
        assertThat(point4.identifier).isEqualTo("point002")
        assertThat(point4.name).isEqualTo("topCurve")
        assertThat(point4.smooth).isEqualTo("yes")

        // Other points have no identifier
        val point2 = points[1]
        assertThat(point2.identifier).isNull()
    }

    @Test
    fun testReadAnchorIdentifierAndColor() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphs = reader.readGlyphs().toList()

        val glyphC = glyphs.find { it.name == "c" }
        assertThat(glyphC).isNotNull()

        val anchors = glyphC!!.anchors
        assertThat(anchors).isNotNull()
        assertThat(anchors).hasSize(2)

        // First anchor has color and identifier
        val anchor1 = anchors!![0]
        assertThat(anchor1.name).isEqualTo("top")
        assertThat(anchor1.color).isEqualTo("1,0,0,0.5")
        assertThat(anchor1.identifier).isEqualTo("anchor001")

        // Second anchor has identifier but no color
        val anchor2 = anchors[1]
        assertThat(anchor2.name).isEqualTo("bottom")
        assertThat(anchor2.color).isNull()
        assertThat(anchor2.identifier).isEqualTo("anchor002")
    }

    @Test
    fun testReadGlyphWithoutIdentifiers() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphs = reader.readGlyphs().toList()

        // Glyph 'a' has no identifiers
        val glyphA = glyphs.find { it.name == "a" }
        assertThat(glyphA).isNotNull()

        val contours = glyphA!!.contours
        assertThat(contours).isNotNull()
        assertThat(contours!![0].identifier).isNull()
        assertThat(contours[0].points[0].identifier).isNull()
    }

    @Test
    fun testWriteAndReadIdentifiers() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        // Create a glyph with identifiers
        val glyph = GlyphValues()
        glyph.name = "test"
        glyph.width = 500f
        glyph.anchors = listOf(
            Anchor(x = 250f, y = 600f, name = "top", color = "0,1,0,1", identifier = "a001")
        )
        glyph.contours = listOf(
            Contour(
                identifier = "c001",
                points = listOf(
                    Point(x = 0f, y = 0f, type = "line", identifier = "p001"),
                    Point(x = 500f, y = 0f, type = "line", name = "corner", identifier = "p002"),
                    Point(x = 250f, y = 700f, type = "line")
                )
            )
        )

        // Write
        run {
            val writer = UFOWriter(memPath)
            writer.writeMetaInfo()
            writer.writeGlyphs(listOf(glyph))
        }

        // Read back
        val reader = UFOReader(memPath)
        val glyphs = reader.readGlyphs().toList()

        assertThat(glyphs).hasSize(1)
        val readGlyph = glyphs[0]

        // Verify anchor
        assertThat(readGlyph.anchors).hasSize(1)
        val anchor = readGlyph.anchors!![0]
        assertThat(anchor.name).isEqualTo("top")
        assertThat(anchor.color).isEqualTo("0,1,0,1")
        assertThat(anchor.identifier).isEqualTo("a001")

        // Verify contour
        assertThat(readGlyph.contours).hasSize(1)
        val contour = readGlyph.contours!![0]
        assertThat(contour.identifier).isEqualTo("c001")

        // Verify points
        val points = contour.points
        assertThat(points[0].identifier).isEqualTo("p001")
        assertThat(points[1].identifier).isEqualTo("p002")
        assertThat(points[1].name).isEqualTo("corner")
        assertThat(points[2].identifier).isNull()
    }

    @Test
    fun testComponentIdentifier() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        // Create base glyph
        val baseGlyph = GlyphValues()
        baseGlyph.name = "a"
        baseGlyph.width = 500f
        baseGlyph.contours = listOf(
            Contour(points = listOf(
                Point(x = 0f, y = 0f, type = "line"),
                Point(x = 500f, y = 0f, type = "line"),
                Point(x = 250f, y = 700f, type = "line")
            ))
        )

        // Create composite glyph with component identifier
        val compositeGlyph = GlyphValues()
        compositeGlyph.name = "aacute"
        compositeGlyph.width = 500f
        compositeGlyph.components = listOf(
            Component(base = "a", identifier = "comp001"),
            Component(base = "acute", xOffset = 100f, yOffset = 200f, identifier = "comp002")
        )

        // Create acute glyph
        val acuteGlyph = GlyphValues()
        acuteGlyph.name = "acute"
        acuteGlyph.width = 200f

        // Write
        run {
            val writer = UFOWriter(memPath)
            writer.writeMetaInfo()
            writer.writeGlyphs(listOf(baseGlyph, compositeGlyph, acuteGlyph))
        }

        // Read back
        val reader = UFOReader(memPath)
        val glyphs = reader.readGlyphs().toList()

        val readComposite = glyphs.find { it.name == "aacute" }
        assertThat(readComposite).isNotNull()
        assertThat(readComposite!!.components).hasSize(2)

        val comp1 = readComposite.components!![0]
        assertThat(comp1.base).isEqualTo("a")
        assertThat(comp1.identifier).isEqualTo("comp001")

        val comp2 = readComposite.components!![1]
        assertThat(comp2.base).isEqualTo("acute")
        assertThat(comp2.identifier).isEqualTo("comp002")
        assertThat(comp2.xOffset).isEqualTo(100f)
        assertThat(comp2.yOffset).isEqualTo(200f)
    }
}

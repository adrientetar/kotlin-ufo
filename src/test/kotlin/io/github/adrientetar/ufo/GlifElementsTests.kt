package io.github.adrientetar.ufo

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import kotlin.test.Test

/**
 * Tests for GLIF guideline, image, and note elements.
 */
class GlifElementsTests {
    @Test
    fun testReadNote() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphs = reader.readGlyphs().toList()

        val glyphA = glyphs.find { it.name == "a" }
        assertThat(glyphA).isNotNull()
        assertThat(glyphA!!.note).isEqualTo("This is a sample note for glyph 'a'.")
    }

    @Test
    fun testReadImage() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphs = reader.readGlyphs().toList()

        val glyphA = glyphs.find { it.name == "a" }
        assertThat(glyphA).isNotNull()
        assertThat(glyphA!!.image).isNotNull()

        val image = glyphA.image!!
        assertThat(image.fileName).isEqualTo("a_sketch.png")
        assertThat(image.xOffset).isEqualTo(10f)
        assertThat(image.yOffset).isEqualTo(20f)
        assertThat(image.xScale).isEqualTo(0.5f)
        assertThat(image.yScale).isEqualTo(0.5f)
        assertThat(image.color).isEqualTo("1,0,0,0.5")
    }

    @Test
    fun testReadGuidelines() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphs = reader.readGlyphs().toList()

        val glyphA = glyphs.find { it.name == "a" }
        assertThat(glyphA).isNotNull()
        assertThat(glyphA!!.guidelines).isNotNull()
        assertThat(glyphA.guidelines).hasSize(3)

        // Vertical guideline (x only)
        val verticalGuide = glyphA.guidelines!![0]
        assertThat(verticalGuide.x).isEqualTo(194f)
        assertThat(verticalGuide.y).isNull()
        assertThat(verticalGuide.name).isEqualTo("center")
        assertThat(verticalGuide.color).isEqualTo("0,0,1,1")

        // Horizontal guideline (y only)
        val horizontalGuide = glyphA.guidelines!![1]
        assertThat(horizontalGuide.x).isNull()
        assertThat(horizontalGuide.y).isEqualTo(510f)
        assertThat(horizontalGuide.name).isEqualTo("top")

        // Diagonal guideline (x, y, and angle)
        val diagonalGuide = glyphA.guidelines!![2]
        assertThat(diagonalGuide.x).isEqualTo(100f)
        assertThat(diagonalGuide.y).isEqualTo(200f)
        assertThat(diagonalGuide.angle).isEqualTo(45f)
        assertThat(diagonalGuide.name).isEqualTo("diagonal")
        assertThat(diagonalGuide.identifier).isEqualTo("guide1")
    }

    @Test
    fun testWriteNote() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        run {
            val writer = UFOWriter(memPath)
            val glyph = GlyphValues().apply {
                name = "test"
                note = "Test note content"
                width = 500f
            }
            writer.writeGlyphs(listOf(glyph))
        }

        val reader = UFOReader(memPath)
        val glyphs = reader.readGlyphs().toList()
        assertThat(glyphs).hasSize(1)
        assertThat(glyphs[0].note).isEqualTo("Test note content")
    }

    @Test
    fun testWriteImage() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        run {
            val writer = UFOWriter(memPath)
            val glyph = GlyphValues().apply {
                name = "test"
                image = Image(
                    fileName = "test.png",
                    xScale = 0.75f,
                    yScale = 0.75f,
                    xOffset = 50f,
                    yOffset = 100f,
                    color = "0,1,0,0.5"
                )
                width = 500f
            }
            writer.writeGlyphs(listOf(glyph))
        }

        val reader = UFOReader(memPath)
        val glyphs = reader.readGlyphs().toList()
        assertThat(glyphs).hasSize(1)

        val image = glyphs[0].image
        assertThat(image).isNotNull()
        assertThat(image!!.fileName).isEqualTo("test.png")
        assertThat(image.xScale).isEqualTo(0.75f)
        assertThat(image.yScale).isEqualTo(0.75f)
        assertThat(image.xOffset).isEqualTo(50f)
        assertThat(image.yOffset).isEqualTo(100f)
        assertThat(image.color).isEqualTo("0,1,0,0.5")
    }

    @Test
    fun testWriteGuidelines() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        run {
            val writer = UFOWriter(memPath)
            val glyph = GlyphValues().apply {
                name = "test"
                guidelines = listOf(
                    GlyphGuideline(x = 100f, name = "vertical"),
                    GlyphGuideline(y = 200f, name = "horizontal"),
                    GlyphGuideline(x = 50f, y = 50f, angle = 30f, name = "angled", identifier = "g1")
                )
                width = 500f
            }
            writer.writeGlyphs(listOf(glyph))
        }

        val reader = UFOReader(memPath)
        val glyphs = reader.readGlyphs().toList()
        assertThat(glyphs).hasSize(1)

        val guidelines = glyphs[0].guidelines
        assertThat(guidelines).isNotNull()
        assertThat(guidelines).hasSize(3)

        assertThat(guidelines!![0].x).isEqualTo(100f)
        assertThat(guidelines[0].name).isEqualTo("vertical")

        assertThat(guidelines[1].y).isEqualTo(200f)
        assertThat(guidelines[1].name).isEqualTo("horizontal")

        assertThat(guidelines[2].x).isEqualTo(50f)
        assertThat(guidelines[2].y).isEqualTo(50f)
        assertThat(guidelines[2].angle).isEqualTo(30f)
        assertThat(guidelines[2].identifier).isEqualTo("g1")
    }

    @Test
    fun testRoundTripWithAllElements() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        // Read from sample font and write to in-memory fs
        run {
            val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
            val reader = UFOReader(ufo)
            val glyphs = reader.readGlyphs().toList()

            val writer = UFOWriter(memPath)
            writer.writeGlyphs(glyphs)
            writer.writeLib(reader.readLib())
        }

        // Read back and verify glyph 'a' has all elements preserved
        val reader = UFOReader(memPath)
        val glyphs = reader.readGlyphs().toList()
        val glyphA = glyphs.find { it.name == "a" }

        assertThat(glyphA).isNotNull()
        assertThat(glyphA!!.note).isEqualTo("This is a sample note for glyph 'a'.")
        assertThat(glyphA.image).isNotNull()
        assertThat(glyphA.image!!.fileName).isEqualTo("a_sketch.png")
        assertThat(glyphA.guidelines).hasSize(3)
    }
}

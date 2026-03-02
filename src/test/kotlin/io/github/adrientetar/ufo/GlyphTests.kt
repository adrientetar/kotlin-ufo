package io.github.adrientetar.ufo

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.writeText
import kotlin.test.Test

class GlyphTests {
    @Test
    fun testInvalidRead() {
        // TODO
    }

    @Test
    fun testPopulate() {
        // Populate glyphs and verify (test the setters)
        val glyphs = mutableListOf<GlyphValues>()
        populateGlyphs(glyphs)
        verifyGlyphs(glyphs)
    }

    @Test
    fun testRead() {
        // Read from the sample font and verify (test the reader)
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphs = reader.readGlyphs()
        verifyGlyphs(glyphs.toList())
    }

    @Test
    fun testWrite() {
        // Set up an in-memory filesystem
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        run {
            // Read from the sample font
            val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
            val reader = UFOReader(ufo)
            val glyphs = reader.readGlyphs()

            // Write to in-memory fs
            val writer = UFOWriter(memPath)
            writer.writeGlyphs(glyphs.toList())
            // Write the lib as well, so we get the glyph order
            writer.writeLib(reader.readLib())
        }

        // Read from in-memory fs and verify (test the writer)
        val reader = UFOReader(memPath)
        val glyphs = reader.readGlyphs()
        verifyGlyphs(glyphs.toList())
    }

    @Test
    fun testReadNonStrictMode() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")
        Files.createDirectories(memPath)

        // In non-strict mode, missing/invalid plists should not throw
        val reader = UFOReader(memPath, strict = false)

        val fontInfo = reader.readFontInfo()
        assertThat(fontInfo.familyName).isNull()

        val lib = reader.readLib()
        assertThat(lib.glyphOrder).isNull()

        val groups = reader.readGroups()
        assertThat(groups.groupNames).isEmpty()

        val kerning = reader.readKerning()
        assertThat(kerning.firstGlyphs).isEmpty()

        val features = reader.readFeatures()
        assertThat(features.isEmpty).isTrue()
    }

    @Test
    fun testReadGlyphsNonStrictWithCorruptGlif() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")
        Files.createDirectories(memPath)

        // Write a valid metainfo, layercontents, and contents.plist but a corrupt GLIF
        val glyphsDir = memPath.resolve("glyphs")
        Files.createDirectories(glyphsDir)

        // Write layercontents.plist
        val layerContents = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<array>
  <array><string>public.default</string><string>glyphs</string></array>
</array>
</plist>"""
        memPath.resolve("layercontents.plist").writeText(layerContents)

        val contentsPlist = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>a</key>
  <string>a.glif</string>
</dict>
</plist>"""
        glyphsDir.resolve("contents.plist").writeText(contentsPlist)
        glyphsDir.resolve("a.glif").writeText("<invalid xml")

        // In non-strict mode, corrupt glyphs should be silently skipped
        val reader = UFOReader(memPath, strict = false)
        val glyphs = reader.readGlyphs().toList()
        assertThat(glyphs).isEmpty()
    }

    private fun populateGlyphs(glyphs: MutableList<GlyphValues>) {
        glyphs.add(
            GlyphValues().apply {
                name = ".notdef"
                contours = listOf(
                    contourOf(
                        Point(450f, 0f, "line"),
                        Point(450f, 750f, "line"),
                        Point(50f, 750f, "line"),
                        Point(50f, 0f, "line"),
                    ),
                    contourOf(
                        Point(400f, 50f, "line"),
                        Point(100f, 50f, "line"),
                        Point(100f, 700f, "line"),
                        Point(400f, 700f, "line"),
                    )
                )
                height = 1000f
                width = 500f
            }
        )
        glyphs.add(
            GlyphValues().apply {
                name = "space"
                unicodes = listOf(0x0020)
            }
        )
    }

    private fun verifyGlyphs(glyphs: List<GlyphValues>) {
        val it = glyphs.iterator()
        it.next().let { glyph ->
            assertThat(glyph.name).isEqualTo(".notdef")
            assertThat(glyph.unicodes).isEqualTo(null)
            assertThat(glyph.anchors).isEqualTo(null)
            assertThat(glyph.components).isEqualTo(null)
            assertThat(glyph.contours).isEqualTo(
                listOf(
                    contourOf(
                        Point(450f, 0f, "line"),
                        Point(450f, 750f, "line"),
                        Point(50f, 750f, "line"),
                        Point(50f, 0f, "line"),
                    ),
                    contourOf(
                        Point(400f, 50f, "line"),
                        Point(100f, 50f, "line"),
                        Point(100f, 700f, "line"),
                        Point(400f, 700f, "line"),
                    )
                )
            )
            assertThat(glyph.height).isEqualTo(1000)
            assertThat(glyph.width).isEqualTo(500)
        }
        it.next().let { glyph ->
            assertThat(glyph.name).isEqualTo("space")
            assertThat(glyph.unicodes).isEqualTo(listOf(0x0020))
        }
    }
}

package io.github.adrientetar.ufo

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test

class GlyphLibTests {

    @Test
    fun testReadGlyphLib() {
        // Read from the sample font
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphs = reader.readGlyphs().toList()

        // Find glyph 'b' which has lib data
        val glyphB = glyphs.find { it.name == "b" }
        assertThat(glyphB).isNotNull()

        val lib = glyphB!!.lib
        assertThat(lib.isEmpty).isFalse()
        assertThat(lib.markColor).isEqualTo("1,0,0,1")
        assertThat(lib.verticalOrigin).isEqualTo(750f)
        assertThat(lib["com.example.customData"]).isEqualTo("some custom value")
    }

    @Test
    fun testGlyphWithoutLib() {
        // Read from the sample font
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphs = reader.readGlyphs().toList()

        // Find glyph 'a' which has no lib data
        val glyphA = glyphs.find { it.name == "a" }
        assertThat(glyphA).isNotNull()

        val lib = glyphA!!.lib
        assertThat(lib.isEmpty).isTrue()
        assertThat(lib.markColor).isNull()
    }

    @Test
    fun testPopulateGlyphLib() {
        val glyph = GlyphValues()
        glyph.name = "test"
        glyph.width = 500f

        // Set lib values
        glyph.lib.markColor = "0,1,0,1"
        glyph.lib.verticalOrigin = 800f
        glyph.lib["com.myapp.notes"] = "Test note"

        // Verify
        assertThat(glyph.lib.markColor).isEqualTo("0,1,0,1")
        assertThat(glyph.lib.verticalOrigin).isEqualTo(800f)
        assertThat(glyph.lib["com.myapp.notes"]).isEqualTo("Test note")
        assertThat(glyph.lib.keys).containsExactly(
            "public.markColor",
            "public.verticalOrigin",
            "com.myapp.notes"
        )
    }

    @Test
    fun testWriteAndReadGlyphLib() {
        // Set up an in-memory filesystem
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        // Create a glyph with lib data
        val originalGlyph = GlyphValues()
        originalGlyph.name = "test"
        originalGlyph.width = 600f
        originalGlyph.lib.markColor = "0.5,0.5,1,1"
        originalGlyph.lib.verticalOrigin = 700f
        originalGlyph.lib["com.test.myKey"] = "myValue"

        // Write the glyph
        run {
            val writer = UFOWriter(memPath)
            writer.writeMetaInfo()
            writer.writeGlyphs(listOf(originalGlyph))
        }

        // Read the glyph back
        val reader = UFOReader(memPath)
        val glyphs = reader.readGlyphs().toList()

        assertThat(glyphs).hasSize(1)
        val readGlyph = glyphs[0]

        assertThat(readGlyph.name).isEqualTo("test")
        assertThat(readGlyph.lib.markColor).isEqualTo("0.5,0.5,1,1")
        assertThat(readGlyph.lib.verticalOrigin).isEqualTo(700f)
        assertThat(readGlyph.lib["com.test.myKey"]).isEqualTo("myValue")
    }

    @Test
    fun testGlyphLibRemove() {
        val glyph = GlyphValues()
        glyph.lib.markColor = "1,0,0,1"
        glyph.lib["custom"] = "value"

        assertThat(glyph.lib.containsKey("public.markColor")).isTrue()
        assertThat(glyph.lib.containsKey("custom")).isTrue()

        glyph.lib.remove("custom")
        assertThat(glyph.lib.containsKey("custom")).isFalse()

        glyph.lib.markColor = null
        assertThat(glyph.lib.containsKey("public.markColor")).isFalse()
    }

    @Test
    fun testEmptyLibNotSerialized() {
        // Set up an in-memory filesystem
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        // Create a glyph without lib data
        val glyph = GlyphValues()
        glyph.name = "nolib"
        glyph.width = 500f

        // Write the glyph
        val writer = UFOWriter(memPath)
        writer.writeMetaInfo()
        writer.writeGlyphs(listOf(glyph))

        // Read the raw GLIF file and verify no lib element
        val glifContent = Files.readString(memPath.resolve("glyphs/nolib.glif"))
        assertThat(glifContent).doesNotContain("<lib>")
    }
}

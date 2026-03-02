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

    @Test
    fun testExtractLibFromGlifXmlNoLib() {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
            <glyph name="test" format="2">
              <advance width="500"/>
              <outline/>
            </glyph>"""
        val dict = extractLibFromGlifXml(xml)
        assertThat(dict.count()).isEqualTo(0)
    }

    @Test
    fun testExtractLibFromGlifXmlWithLib() {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
            <glyph name="test" format="2">
              <advance width="500"/>
              <outline/>
              <lib>
                <dict>
                  <key>public.markColor</key>
                  <string>1,0,0,1</string>
                </dict>
              </lib>
            </glyph>"""
        val dict = extractLibFromGlifXml(xml)
        assertThat(dict.count()).isEqualTo(1)
        assertThat(dict.getString("public.markColor")).isEqualTo("1,0,0,1")
    }

    @Test
    fun testSerializeLibToXml() {
        val dict = com.dd.plist.NSDictionary()
        dict.put("public.markColor", "0,1,0,1")
        val xml = serializeLibToXml(dict)
        assertThat(xml).contains("<lib>")
        assertThat(xml).contains("</lib>")
        assertThat(xml).contains("public.markColor")
        assertThat(xml).contains("0,1,0,1")
    }

    @Test
    fun testGlyphLibSetNull() {
        val glyph = GlyphValues()
        glyph.lib["key1"] = "value1"
        glyph.lib["key2"] = "value2"
        assertThat(glyph.lib.keys).hasSize(2)

        glyph.lib["key1"] = null
        assertThat(glyph.lib.containsKey("key1")).isFalse()
        assertThat(glyph.lib.keys).hasSize(1)
    }

    @Test
    fun testGlifLibSerializerRoundTrip() {
        // Test the serializer indirectly through write/read with multiple lib entries
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        val glyph = GlyphValues().apply {
            name = "sertest"
            width = 500f
        }
        glyph.lib.markColor = "1,0,0,1"
        glyph.lib.verticalOrigin = 800f
        glyph.lib["com.example.int"] = 42
        glyph.lib["com.example.bool"] = true

        val writer = UFOWriter(memPath)
        writer.writeMetaInfo()
        writer.writeGlyphs(listOf(glyph))

        val reader = UFOReader(memPath)
        val readGlyph = reader.readGlyphs().first()

        assertThat(readGlyph.lib.markColor).isEqualTo("1,0,0,1")
        assertThat(readGlyph.lib.verticalOrigin).isEqualTo(800f)
        assertThat(readGlyph.lib["com.example.int"]).isEqualTo(42L)
        assertThat(readGlyph.lib["com.example.bool"]).isEqualTo(true)
    }

    @Test
    fun testGlifLibSerializerEmpty() {
        // Verify an empty GlifLib produces no lib element
        val glifLib = GlifLib()
        assertThat(glifLib.content.count()).isEqualTo(0)
    }

    @Test
    fun testFeaturesValuesToString() {
        val short = FeaturesValues("short text")
        assertThat(short.toString()).contains("short text")

        val long = FeaturesValues("a".repeat(100))
        assertThat(long.toString()).contains("...")
    }

    @Test
    fun testFeaturesValuesBlankIsEmpty() {
        val blank = FeaturesValues("   ")
        assertThat(blank.isEmpty).isTrue()
    }
}

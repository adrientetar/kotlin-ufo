package io.github.adrientetar.ufo

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.writeText
import kotlin.test.Test

/**
 * Integration tests for reading UFO 2 fonts via [UFOReader].
 */
class UFO2ReaderTests {

    @Test
    fun `detects format version 2`() {
        val ufo = Paths.get(getResourceURI("/TestFontUFO2.ufo"))
        val reader = UFOReader(ufo)

        assertThat(reader.formatVersion).isEqualTo(2)
    }

    @Test
    fun `detects format version 3`() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)

        assertThat(reader.formatVersion).isEqualTo(3)
    }

    @Test
    fun `rejects unsupported format version`() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/BadFont.ufo")
        Files.createDirectories(memPath)

        memPath.resolve("metainfo.plist").writeText("""
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>creator</key>
                <string>test</string>
                <key>formatVersion</key>
                <integer>1</integer>
            </dict>
            </plist>
        """.trimIndent())

        val reader = UFOReader(memPath)

        assertThrows<UFOLibException> {
            reader.formatVersion
        }
    }

    @Test
    fun `reads layer contents for UFO 2 as single default layer`() {
        val ufo = Paths.get(getResourceURI("/TestFontUFO2.ufo"))
        val reader = UFOReader(ufo)

        val layers = reader.readLayerContents()

        assertThat(layers).hasSize(1)
        assertThat(layers[0].first).isEqualTo(Layer.DEFAULT_NAME)
        assertThat(layers[0].second).isEqualTo(Layer.DEFAULT_DIRECTORY)
    }

    @Test
    fun `reads fontinfo from UFO 2`() {
        val ufo = Paths.get(getResourceURI("/TestFontUFO2.ufo"))
        val reader = UFOReader(ufo)

        val info = reader.readFontInfo()

        assertThat(info.familyName).isEqualTo("Test Family")
        assertThat(info.styleName).isEqualTo("Regular")
        assertThat(info.unitsPerEm).isEqualTo(1000)
        assertThat(info.year).isEqualTo(2020)
    }

    @Test
    fun `reads glyphs from UFO 2 and converts anchors`() {
        val ufo = Paths.get(getResourceURI("/TestFontUFO2.ufo"))
        val reader = UFOReader(ufo)

        val glyphs = reader.readGlyphs().toList()

        // Should have 2 glyphs: space and a (per glyph order)
        assertThat(glyphs).hasSize(2)

        val space = glyphs.find { it.name == "space" }!!
        assertThat(space.width).isEqualTo(250f)

        val a = glyphs.find { it.name == "a" }!!
        assertThat(a.width).isEqualTo(388f)

        // Anchors should have been extracted from single-point move contours
        assertThat(a.anchors).hasSize(2)
        assertThat(a.anchors!![0]).isEqualTo(Anchor(x = 194f, y = 650f, name = "top"))
        assertThat(a.anchors!![1]).isEqualTo(Anchor(x = 0f, y = 0f, name = "bottom"))

        // Only the real contour should remain
        assertThat(a.contours).hasSize(1)
        assertThat(a.contours!![0].points).hasSize(3)
    }

    @Test
    fun `GlyphSet reads UFO 2 glyphs and converts anchors`() {
        val ufo = Paths.get(getResourceURI("/TestFontUFO2.ufo"))
        val reader = UFOReader(ufo)

        val glyphSet = reader.getGlyphSet()
        val a = glyphSet["a"]!!

        assertThat(a.anchors).hasSize(2)
        assertThat(a.anchors!![0].name).isEqualTo("top")
        assertThat(a.anchors!![1].name).isEqualTo("bottom")
        assertThat(a.contours).hasSize(1)
    }

    @Test
    fun `converts kerning groups from UFO 2`() {
        val ufo = Paths.get(getResourceURI("/TestFontUFO2.ufo"))
        val reader = UFOReader(ufo)

        val (groups, kerning) = reader.readConvertedGroupsAndKerning()

        // Groups should have new public.kern* names added
        assertThat(groups.dict.containsKey("public.kern1.A")).isTrue()
        assertThat(groups.dict.containsKey("public.kern2.V")).isTrue()

        // Kerning should reference new group names
        assertThat(kerning.dict.containsKey("public.kern1.A")).isTrue()
        val seconds = kerning.dict["public.kern1.A"]!!.toDictionary()
        assertThat(seconds.containsKey("public.kern2.V")).isTrue()

        // Glyph-to-glyph kerning should be unchanged
        assertThat(kerning.dict.containsKey("a")).isTrue()
    }

    @Test
    fun `readMetaInfo works for UFO 2`() {
        val ufo = Paths.get(getResourceURI("/TestFontUFO2.ufo"))
        val reader = UFOReader(ufo)

        val meta = reader.readMetaInfo()

        assertThat(meta.creator).isEqualTo("org.robofab.ufoLib")
        assertThat(meta.formatVersion).isEqualTo(2)
    }

    @Test
    fun `layerNames returns single layer for UFO 2`() {
        val ufo = Paths.get(getResourceURI("/TestFontUFO2.ufo"))
        val reader = UFOReader(ufo)

        val names = reader.layerNames()

        assertThat(names).containsExactly(Layer.DEFAULT_NAME)
    }
}

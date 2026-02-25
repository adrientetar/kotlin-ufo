package io.github.adrientetar.ufo

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths
import kotlin.test.Test

/**
 * Tests for UFOZ (ZIP-compressed UFO) reading via [UFOReader.open].
 */
class UFOZReaderTests {

    @Test
    fun `reads metainfo from ufoz`() {
        val ufoz = Paths.get(getResourceURI("/TestFont.ufoz"))

        UFOReader.open(ufoz).use { reader ->
            val meta = reader.readMetaInfo()

            assertThat(meta.creator).isEqualTo("org.robofab.ufoLib")
            assertThat(meta.formatVersion).isEqualTo(3)
        }
    }

    @Test
    fun `reads fontinfo from ufoz`() {
        val ufoz = Paths.get(getResourceURI("/TestFont.ufoz"))

        UFOReader.open(ufoz).use { reader ->
            val info = reader.readFontInfo()

            assertThat(info.familyName).isEqualTo("Some Font (Family Name)")
        }
    }

    @Test
    fun `reads glyphs from ufoz`() {
        val ufoz = Paths.get(getResourceURI("/TestFont.ufoz"))

        UFOReader.open(ufoz).use { reader ->
            val glyphs = reader.readGlyphs().toList()

            assertThat(glyphs).isNotEmpty()
            val a = glyphs.find { it.name == "a" }
            assertThat(a).isNotNull()
            assertThat(a!!.width).isEqualTo(388f)
        }
    }

    @Test
    fun `GlyphSet works from ufoz`() {
        val ufoz = Paths.get(getResourceURI("/TestFont.ufoz"))

        UFOReader.open(ufoz).use { reader ->
            val glyphSet = reader.getGlyphSet()

            assertThat(glyphSet.size).isGreaterThan(0)
            val a = glyphSet["a"]
            assertThat(a).isNotNull()
            assertThat(a!!.contours).isNotNull()
        }
    }

    @Test
    fun `reads layer contents from ufoz`() {
        val ufoz = Paths.get(getResourceURI("/TestFont.ufoz"))

        UFOReader.open(ufoz).use { reader ->
            val layers = reader.readLayerContents()

            assertThat(layers).hasSize(2)
            assertThat(layers[0].first).isEqualTo("public.default")
            assertThat(layers[1].first).isEqualTo("public.background")
        }
    }

    @Test
    fun `reads groups and kerning from ufoz`() {
        val ufoz = Paths.get(getResourceURI("/TestFont.ufoz"))

        UFOReader.open(ufoz).use { reader ->
            val groups = reader.readGroups()
            val kerning = reader.readKerning()

            assertThat(groups.dict.count()).isGreaterThan(0)
            assertThat(kerning.dict.count()).isGreaterThan(0)
        }
    }

    @Test
    fun `reads features from ufoz`() {
        val ufoz = Paths.get(getResourceURI("/TestFont.ufoz"))

        UFOReader.open(ufoz).use { reader ->
            val features = reader.readFeatures()

            assertThat(features.isEmpty).isFalse()
        }
    }

    @Test
    fun `open works with directory path`() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))

        UFOReader.open(ufo).use { reader ->
            val meta = reader.readMetaInfo()
            assertThat(meta.formatVersion).isEqualTo(3)
        }
    }
}

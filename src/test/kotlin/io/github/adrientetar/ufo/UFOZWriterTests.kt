package io.github.adrientetar.ufo

import com.google.common.truth.Truth.assertThat
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.deleteIfExists
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * Tests for [UFOZWriter] — writing UFOZ (ZIP-compressed UFO) files.
 */
class UFOZWriterTests {

    private val tempFile = Files.createTempFile("test-", ".ufoz")

    @AfterTest
    fun cleanup() {
        tempFile.deleteIfExists()
    }

    @Test
    fun `round-trip write and read ufoz`() {
        // Write
        UFOZWriter.open(tempFile).use { writer ->
            writer.writeMetaInfo()
            writer.writeFontInfo(FontInfoValues().apply {
                familyName = "UFOZ Test"
                unitsPerEm = 1000
            })
            writer.writeGlyphs(listOf(
                GlyphValues().apply {
                    name = "space"
                    width = 250f
                },
                GlyphValues().apply {
                    name = "a"
                    width = 500f
                    contours = listOf(
                        contourOf(
                            Point(0f, 0f, "line"),
                            Point(250f, 700f, "line"),
                            Point(500f, 0f, "line")
                        )
                    )
                }
            ))
            writer.writeLib(LibValues().apply {
                glyphOrder = listOf("space", "a")
            })
        }

        // Read back
        UFOReader.open(tempFile).use { reader ->
            val meta = reader.readMetaInfo()
            assertThat(meta.formatVersion).isEqualTo(3)

            val info = reader.readFontInfo()
            assertThat(info.familyName).isEqualTo("UFOZ Test")
            assertThat(info.unitsPerEm).isEqualTo(1000)

            val glyphs = reader.readGlyphs().toList()
            assertThat(glyphs).hasSize(2)
            assertThat(glyphs[0].name).isEqualTo("space")
            assertThat(glyphs[1].name).isEqualTo("a")
            assertThat(glyphs[1].width).isEqualTo(500f)
            assertThat(glyphs[1].contours).hasSize(1)
        }
    }

    @Test
    fun `round-trip with existing TestFont`() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val srcReader = UFOReader(ufo)

        // Write the test font as UFOZ
        UFOZWriter.open(tempFile).use { writer ->
            writer.writeMetaInfo()
            writer.writeFontInfo(srcReader.readFontInfo())
            writer.writeGlyphs(srcReader.readGlyphs().toList())
            writer.writeGroups(srcReader.readGroups())
            writer.writeKerning(srcReader.readKerning())
            writer.writeLib(srcReader.readLib())
            writer.writeFeatures(srcReader.readFeatures())
        }

        // Read back and verify
        UFOReader.open(tempFile).use { reader ->
            val info = reader.readFontInfo()
            assertThat(info.familyName).isEqualTo(srcReader.readFontInfo().familyName)

            val glyphs = reader.readGlyphs().toList()
            assertThat(glyphs).isNotEmpty()

            val groups = reader.readGroups()
            assertThat(groups.dict.count()).isGreaterThan(0)

            val kerning = reader.readKerning()
            assertThat(kerning.dict.count()).isGreaterThan(0)

            val features = reader.readFeatures()
            assertThat(features.isEmpty).isFalse()
        }
    }
}

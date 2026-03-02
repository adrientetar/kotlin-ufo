package io.github.adrientetar.ufo

import com.dd.plist.NSDictionary
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test

class LayerTests {
    @Test
    fun testLayerCreation() {
        val layer = Layer.createDefault()
        assertThat(layer.name).isEqualTo(Layer.DEFAULT_NAME)
        assertThat(layer.directoryName).isEqualTo(Layer.DEFAULT_DIRECTORY)
        assertThat(layer.isDefault).isTrue()
        assertThat(layer.glyphs).isEmpty()
    }

    @Test
    fun testBackgroundLayerCreation() {
        val layer = Layer.createBackground()
        assertThat(layer.name).isEqualTo(Layer.BACKGROUND_NAME)
        assertThat(layer.directoryName).isEqualTo("glyphs.public.background")
        assertThat(layer.isDefault).isFalse()
    }

    @Test
    fun testLayerNameToDirectoryName() {
        assertThat(layerNameToDirectoryName(Layer.DEFAULT_NAME)).isEqualTo("glyphs")
        assertThat(layerNameToDirectoryName("Sketches")).isEqualTo("glyphs.S_ketches")
        assertThat(layerNameToDirectoryName("public.background")).isEqualTo("glyphs.public.background")
    }

    @Test
    fun testReadLayerContents() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)

        val layerContents = reader.readLayerContents()
        assertThat(layerContents).hasSize(2)
        assertThat(layerContents[0]).isEqualTo("public.default" to "fore")
        assertThat(layerContents[1]).isEqualTo("public.background" to "glyphs.background")
    }

    @Test
    fun testLayerNames() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)

        val layerNames = reader.layerNames()
        assertThat(layerNames).containsExactly("public.default", "public.background")
    }

    @Test
    fun testGetGlyphSets() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)

        val glyphSets = reader.getGlyphSets()
        assertThat(glyphSets).hasSize(2)

        // Default layer
        val defaultGlyphSet = glyphSets["public.default"]
        assertThat(defaultGlyphSet).isNotNull()
        assertThat(defaultGlyphSet!!.size).isGreaterThan(0)
        assertThat(defaultGlyphSet.glyphNames).contains("a")

        // Background layer
        val backgroundGlyphSet = glyphSets["public.background"]
        assertThat(backgroundGlyphSet).isNotNull()
        assertThat(backgroundGlyphSet!!.size).isEqualTo(1)
        assertThat(backgroundGlyphSet.glyphNames).containsExactly("a")
    }

    @Test
    fun testGetGlyphSet() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)

        val defaultGlyphSet = reader.getGlyphSet()
        assertThat(defaultGlyphSet.size).isGreaterThan(0)
    }

    @Test
    fun testGetGlyphSetByName() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)

        val backgroundGlyphSet = reader.getGlyphSet("public.background")
        assertThat(backgroundGlyphSet).isNotNull()
        assertThat(backgroundGlyphSet!!.size).isEqualTo(1)
        assertThat(backgroundGlyphSet["a"]).isNotNull()
        assertThat(backgroundGlyphSet["a"]!!.name).isEqualTo("a")
    }

    @Test
    fun testReadLayerInfo() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)

        val backgroundInfo = reader.readLayerInfo("glyphs.background")
        assertThat(backgroundInfo).isNotNull()
        assertThat(backgroundInfo!!.color).isEqualTo("0,0,1,0.5")
    }

    @Test
    fun testGetGlyphSetsWithLayerInfo() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)

        val backgroundInfo = reader.readLayerInfo("glyphs.background")
        assertThat(backgroundInfo).isNotNull()
        assertThat(backgroundInfo!!.color).isEqualTo("0,0,1,0.5")
    }

    @Test
    fun testWriteLayers() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        // Create layers with glyphs
        val defaultGlyph = GlyphValues().apply {
            name = "a"
            width = 500f
            contours = listOf(contourOf(
                Point(0f, 0f, "line"),
                Point(500f, 0f, "line"),
                Point(250f, 700f, "line")
            ))
        }

        val backgroundGlyph = GlyphValues().apply {
            name = "a"
            width = 500f
            contours = listOf(contourOf(
                Point(50f, 0f, "line"),
                Point(450f, 0f, "line"),
                Point(250f, 650f, "line")
            ))
        }

        val defaultLayer = Layer(
            name = Layer.DEFAULT_NAME,
            directoryName = Layer.DEFAULT_DIRECTORY,
            glyphs = mutableListOf(defaultGlyph)
        )

        val backgroundLayer = Layer(
            name = Layer.BACKGROUND_NAME,
            directoryName = "glyphs.public.background",
            glyphs = mutableListOf(backgroundGlyph),
            info = LayerInfo(color = "1,0,0,0.5")
        )

        // Write
        val writer = UFOWriter(memPath)
        writer.writeLayers(listOf(defaultLayer, backgroundLayer))

        // Verify
        val reader = UFOReader(memPath)

        val layerContents = reader.readLayerContents()
        assertThat(layerContents).hasSize(2)
        assertThat(layerContents[0].first).isEqualTo(Layer.DEFAULT_NAME)
        assertThat(layerContents[1].first).isEqualTo(Layer.BACKGROUND_NAME)

        val glyphSets = reader.getGlyphSets()
        assertThat(glyphSets).hasSize(2)

        // Verify default layer
        val defaultGlyphSet = glyphSets[Layer.DEFAULT_NAME]
        assertThat(defaultGlyphSet).isNotNull()
        assertThat(defaultGlyphSet!!.size).isEqualTo(1)
        assertThat(defaultGlyphSet["a"]).isNotNull()
        assertThat(defaultGlyphSet["a"]!!.name).isEqualTo("a")

        // Verify background layer
        val backgroundGlyphSet = glyphSets[Layer.BACKGROUND_NAME]
        assertThat(backgroundGlyphSet).isNotNull()
        assertThat(backgroundGlyphSet!!.size).isEqualTo(1)
        val backgroundInfo = reader.readLayerInfo("glyphs.public.background")
        assertThat(backgroundInfo).isNotNull()
        assertThat(backgroundInfo!!.color).isEqualTo("1,0,0,0.5")
    }

    @Test
    fun testRoundTripLayers() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        // Read from sample font using lazy GlyphSets
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val originalReader = UFOReader(ufo)
        val originalGlyphSets = originalReader.getGlyphSets()
        val originalLayerContents = originalReader.readLayerContents()

        // Convert to Layers for writing
        val layersToWrite = originalLayerContents.map { (layerName, directoryName) ->
            val glyphSet = originalGlyphSets[layerName]!!
            val glyphs = glyphSet.asSequence().toMutableList()
            val layerInfo = originalReader.readLayerInfo(directoryName)
            Layer(
                name = layerName,
                directoryName = directoryName,
                glyphs = glyphs,
                info = layerInfo
            )
        }

        // Write to in-memory fs
        val writer = UFOWriter(memPath)
        writer.writeLayers(layersToWrite)
        writer.writeLib(originalReader.readLib())

        // Read back and verify
        val reader = UFOReader(memPath)
        val readGlyphSets = reader.getGlyphSets()

        assertThat(readGlyphSets).hasSize(2)
        assertThat(readGlyphSets.keys).containsExactly("public.default", "public.background")

        // Verify glyph counts match
        assertThat(readGlyphSets["public.default"]!!.size).isEqualTo(originalGlyphSets["public.default"]!!.size)
        assertThat(readGlyphSets["public.background"]!!.size).isEqualTo(originalGlyphSets["public.background"]!!.size)

        // Verify layer info preserved
        val originalBackgroundInfo = originalReader.readLayerInfo("glyphs.background")
        val readBackgroundInfo = reader.readLayerInfo("glyphs.background")
        assertThat(readBackgroundInfo?.color).isEqualTo(originalBackgroundInfo?.color)
    }

    @Test
    fun testLayerGetGlyph() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphSet = reader.getGlyphSet()

        val glyphA = glyphSet["a"]
        assertThat(glyphA).isNotNull()
        assertThat(glyphA!!.name).isEqualTo("a")

        val glyphMissing = glyphSet["nonexistent"]
        assertThat(glyphMissing).isNull()
    }

    @Test
    fun testLayerInfoEmpty() {
        val emptyInfo = LayerInfo()
        assertThat(emptyInfo.isEmpty).isTrue()

        val infoWithColor = LayerInfo(color = "1,0,0,1")
        assertThat(infoWithColor.isEmpty).isFalse()

        val infoWithLib = LayerInfo(lib = NSDictionary().apply { put("key", "value") })
        assertThat(infoWithLib.isEmpty).isFalse()

        val infoWithEmptyLib = LayerInfo(lib = NSDictionary())
        assertThat(infoWithEmptyLib.isEmpty).isTrue()

        val infoWithNullLib = LayerInfo(color = null, lib = null)
        assertThat(infoWithNullLib.isEmpty).isTrue()
    }

    @Test
    fun testReadLayers() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)

        val layers = reader.readLayers()
        assertThat(layers).hasSize(2)

        // Default layer
        assertThat(layers[0].name).isEqualTo("public.default")
        assertThat(layers[0].directoryName).isEqualTo("fore")
        assertThat(layers[0].isDefault).isFalse() // "fore" != "glyphs"
        assertThat(layers[0].glyphs).isNotEmpty()
        assertThat(layers[0].glyphNames()).contains("a")

        // Background layer
        assertThat(layers[1].name).isEqualTo("public.background")
        assertThat(layers[1].directoryName).isEqualTo("glyphs.background")
        assertThat(layers[1].glyphs).hasSize(1)
        assertThat(layers[1].glyphNames()).containsExactly("a")
        assertThat(layers[1].info).isNotNull()
        assertThat(layers[1].info!!.color).isEqualTo("0,0,1,0.5")
    }

    @Test
    fun testReadGlyphsByLayerName() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)

        val backgroundGlyphs = reader.readGlyphs("public.background").toList()
        assertThat(backgroundGlyphs).hasSize(1)
        assertThat(backgroundGlyphs[0].name).isEqualTo("a")
    }

    @Test
    fun testReadGlyphsNonexistentLayer() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)

        val glyphs = reader.readGlyphs("nonexistent.layer").toList()
        assertThat(glyphs).isEmpty()
    }

    @Test
    fun testReadLayersRoundTrip() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        // Read layers from sample font
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val originalReader = UFOReader(ufo)
        val layers = originalReader.readLayers()

        // Write them back
        val writer = UFOWriter(memPath)
        writer.writeLayers(layers)
        writer.writeLib(originalReader.readLib())

        // Read back and verify
        val reader = UFOReader(memPath)
        val roundTrippedLayers = reader.readLayers()

        assertThat(roundTrippedLayers).hasSize(layers.size)
        for (i in layers.indices) {
            assertThat(roundTrippedLayers[i].name).isEqualTo(layers[i].name)
            assertThat(roundTrippedLayers[i].glyphs.size).isEqualTo(layers[i].glyphs.size)
            assertThat(roundTrippedLayers[i].info?.color).isEqualTo(layers[i].info?.color)
        }
    }

    @Test
    fun testWriteLayerInfoNotWrittenWhenEmpty() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        val layer = Layer(
            name = Layer.DEFAULT_NAME,
            directoryName = Layer.DEFAULT_DIRECTORY,
            glyphs = mutableListOf(GlyphValues().apply { name = "a"; width = 500f }),
            info = LayerInfo() // Empty info
        )

        val writer = UFOWriter(memPath)
        writer.writeLayers(listOf(layer))

        // Verify layerinfo.plist was not created
        val layerInfoPath = memPath.resolve("glyphs/layerinfo.plist")
        assertThat(Files.exists(layerInfoPath)).isFalse()
    }
}

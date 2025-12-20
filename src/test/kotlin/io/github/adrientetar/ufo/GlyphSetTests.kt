package io.github.adrientetar.ufo

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertFailsWith

class GlyphSetTests {

    // The test font has 14 glyphs in the foreground layer
    private val expectedGlyphCount = 14
    // The glyph order includes glyph1/glyph2 which don't exist, so they are filtered out for actual glyph names
    private val expectedGlyphOrder = listOf(
        ".notdef", "space", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"
    )

    @Test
    fun testLazyLoadingSingleGlyph() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphSet = reader.getGlyphSet()

        // Load a single glyph
        val notdef = glyphSet[".notdef"]
        assertThat(notdef).isNotNull()
        assertThat(notdef!!.name).isEqualTo(".notdef")

        // Verify cache has only one glyph
        assertThat(glyphSet.cacheSize).isEqualTo(1)
    }

    @Test
    fun testGlyphSetContains() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphSet = reader.getGlyphSet()

        assertThat(".notdef" in glyphSet).isTrue()
        assertThat("space" in glyphSet).isTrue()
        assertThat("a" in glyphSet).isTrue()
        assertThat("nonexistent" in glyphSet).isFalse()
    }

    @Test
    fun testGlyphSetSize() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphSet = reader.getGlyphSet()

        assertThat(glyphSet.size).isEqualTo(expectedGlyphCount)
        assertThat(glyphSet.isEmpty).isFalse()
    }

    @Test
    fun testGlyphNames() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphSet = reader.getGlyphSet()

        // Names should be in glyph order (filtered to only existing glyphs)
        assertThat(glyphSet.glyphNames).containsExactlyElementsIn(expectedGlyphOrder).inOrder()
    }

    @Test
    fun testGetValue() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphSet = reader.getGlyphSet()

        val glyph = glyphSet.getValue(".notdef")
        assertThat(glyph.name).isEqualTo(".notdef")

        assertFailsWith<NoSuchElementException> {
            glyphSet.getValue("nonexistent")
        }
    }

    @Test
    fun testGetAll() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphSet = reader.getGlyphSet()

        val glyphs = glyphSet.getAll(listOf(".notdef", "space", "nonexistent"))
        assertThat(glyphs).hasSize(2)
        assertThat(glyphs.map { it.name }).containsExactly(".notdef", "space")
    }

    @Test
    fun testGetAllOrNull() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphSet = reader.getGlyphSet()

        val glyphs = glyphSet.getAllOrNull(listOf(".notdef", "nonexistent", "space"))
        assertThat(glyphs).hasSize(3)
        assertThat(glyphs[0]?.name).isEqualTo(".notdef")
        assertThat(glyphs[1]).isNull()
        assertThat(glyphs[2]?.name).isEqualTo("space")
    }

    @Test
    fun testIterator() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphSet = reader.getGlyphSet()

        val names = mutableListOf<String>()
        for (glyph in glyphSet) {
            names.add(glyph.name!!)
        }

        assertThat(names).containsExactlyElementsIn(expectedGlyphOrder).inOrder()
    }

    @Test
    fun testSequence() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphSet = reader.getGlyphSet()

        val names = glyphSet.asSequence()
            .map { it.name }
            .toList()

        assertThat(names).containsExactlyElementsIn(expectedGlyphOrder).inOrder()
    }

    @Test
    fun testAsSequenceToList() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphSet = reader.getGlyphSet()

        val glyphs = glyphSet.asSequence().toList()
        assertThat(glyphs).hasSize(expectedGlyphCount)
        assertThat(glyphs.map { it.name }).containsExactlyElementsIn(expectedGlyphOrder).inOrder()
    }

    @Test
    fun testCaching() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphSet = reader.getGlyphSet()

        // Initial cache is empty
        assertThat(glyphSet.cacheSize).isEqualTo(0)

        // Load a glyph
        val glyph1 = glyphSet[".notdef"]
        assertThat(glyphSet.cacheSize).isEqualTo(1)

        // Load same glyph again - should return cached version
        val glyph2 = glyphSet[".notdef"]
        assertThat(glyph1).isSameInstanceAs(glyph2)
        assertThat(glyphSet.cacheSize).isEqualTo(1)

        // Load another glyph
        glyphSet["space"]
        assertThat(glyphSet.cacheSize).isEqualTo(2)

        // Clear cache
        glyphSet.clearCache()
        assertThat(glyphSet.cacheSize).isEqualTo(0)
    }

    @Test
    fun testDisableCache() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphSet = reader.getGlyphSet()

        // Disable caching
        glyphSet.setCacheEnabled(false)
        assertThat(glyphSet.isCacheEnabled()).isFalse()

        // Load a glyph - should not cache
        glyphSet[".notdef"]
        assertThat(glyphSet.cacheSize).isEqualTo(0)

        // Re-enable caching
        glyphSet.setCacheEnabled(true)
        glyphSet[".notdef"]
        assertThat(glyphSet.cacheSize).isEqualTo(1)
    }

    @Test
    fun testPreload() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphSet = reader.getGlyphSet()

        // Preload specific glyphs
        glyphSet.preload(listOf(".notdef"))
        assertThat(glyphSet.cacheSize).isEqualTo(1)

        // Preload all glyphs
        glyphSet.clearCache()
        glyphSet.preload()
        assertThat(glyphSet.cacheSize).isEqualTo(expectedGlyphCount)
    }

    @Test
    fun testReadGlyphConvenienceMethod() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)

        // Single glyph read
        val glyph = reader.readGlyph(".notdef")
        assertThat(glyph).isNotNull()
        assertThat(glyph!!.name).isEqualTo(".notdef")

        // Non-existent glyph
        val missing = reader.readGlyph("nonexistent")
        assertThat(missing).isNull()
    }

    @Test
    fun testGlyphSetWithWrittenUFO() {
        // Set up an in-memory filesystem
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        // Write some glyphs
        run {
            val writer = UFOWriter(memPath)
            writer.writeMetaInfo()
            writer.writeGlyphs(listOf(
                GlyphValues().apply {
                    name = "a"
                    width = 500f
                },
                GlyphValues().apply {
                    name = "b"
                    width = 600f
                },
                GlyphValues().apply {
                    name = "c"
                    width = 700f
                }
            ))
            writer.writeLib(LibValues().apply {
                glyphOrder = listOf("a", "b", "c")
            })
        }

        // Read back with lazy loading
        val reader = UFOReader(memPath)
        val glyphSet = reader.getGlyphSet()

        assertThat(glyphSet.size).isEqualTo(3)
        assertThat(glyphSet.glyphNames).containsExactly("a", "b", "c").inOrder()

        // Load individual glyphs
        assertThat(glyphSet["a"]?.width).isEqualTo(500f)
        assertThat(glyphSet["b"]?.width).isEqualTo(600f)
        assertThat(glyphSet["c"]?.width).isEqualTo(700f)

        // Only accessed glyphs should be cached
        assertThat(glyphSet.cacheSize).isEqualTo(3)
    }

    @Test
    fun testMissingGlyph() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val glyphSet = reader.getGlyphSet()

        val missing = glyphSet["this_glyph_does_not_exist"]
        assertThat(missing).isNull()
    }

    @Test
    fun testGetGlyphSetForLayer() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)

        // Get default layer
        val defaultSet = reader.getGlyphSet()
        assertThat(defaultSet.size).isEqualTo(expectedGlyphCount)

        // Get background layer (if it exists)
        val backgroundSet = reader.getGlyphSet("public.background")
        assertThat(backgroundSet).isNotNull()

        // Non-existent layer
        val missingSet = reader.getGlyphSet("nonexistent.layer")
        assertThat(missingSet).isNull()
    }

    @Test
    fun testGetGlyphSets() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)

        val glyphSets = reader.getGlyphSets()
        assertThat(glyphSets).isNotEmpty()

        // Should have at least the foreground layer (named "public.default")
        assertThat(glyphSets.keys).contains("public.default")
    }

    @Test
    fun testReadGlyphFromLayer() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)

        // Read from foreground (layer name is "public.default")
        val glyph = reader.readGlyph(".notdef", "public.default")
        assertThat(glyph).isNotNull()
        assertThat(glyph!!.name).isEqualTo(".notdef")

        // Non-existent layer
        val missing = reader.readGlyph(".notdef", "nonexistent.layer")
        assertThat(missing).isNull()
    }
}

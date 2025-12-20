package io.github.adrientetar.ufo

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.writeText
import kotlin.test.Test

class KerningTests {
    @Test
    fun testInvalidRead() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/path/to/TestFont.ufo")

        Files.createDirectories(memPath)
        val kerningPath = memPath.resolve("kerning.plist")
        val reader = UFOReader(memPath)

        // When kerning.plist is missing, pass as it's optional
        val emptyKerning = reader.readKerning()
        assertThat(emptyKerning.firstGlyphs).isEmpty()

        // When kerning.plist is empty, throw UFOLibException
        Files.createFile(kerningPath)
        assertThrows<UFOLibException> {
            reader.readKerning()
        }

        // When kerning.plist is invalid, throw UFOLibException
        kerningPath.writeText("<foo></bar>")
        assertThrows<UFOLibException> {
            reader.readKerning()
        }
    }

    @Test
    fun testPopulateKerning() {
        // Populate kerning and verify (test the setters)
        val kerning = KerningValues()
        populateKerning(kerning)
        verifyKerning(kerning)
    }

    @Test
    fun testReadKerning() {
        // Read from the sample font and verify (test the reader)
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val kerning = reader.readKerning()
        verifyKerning(kerning)
    }

    @Test
    fun testWriteKerning() {
        // Set up an in-memory filesystem
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        run {
            // Read from the sample font
            val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
            val reader = UFOReader(ufo)
            val kerning = reader.readKerning()

            // Write to in-memory fs
            val writer = UFOWriter(memPath)
            writer.writeKerning(kerning)
        }

        // Read from in-memory fs and verify (test the writer)
        val reader = UFOReader(memPath)
        val kerning = reader.readKerning()
        verifyKerning(kerning)
    }

    @Test
    fun testSetAndRemovePairs() {
        val kerning = KerningValues()

        // Set individual pairs
        kerning.set("A", "V", -50f)
        kerning.set("A", "W", -40f)
        kerning.set("T", "o", -30f)

        assertThat(kerning.get("A", "V")).isEqualTo(-50f)
        assertThat(kerning.get("A", "W")).isEqualTo(-40f)
        assertThat(kerning.get("T", "o")).isEqualTo(-30f)
        assertThat(kerning.pairCount).isEqualTo(3)

        // Remove individual pair
        kerning.remove("A", "V")
        assertThat(kerning.get("A", "V")).isNull()
        assertThat(kerning.get("A", "W")).isEqualTo(-40f)
        assertThat(kerning.pairCount).isEqualTo(2)

        // Remove all pairs for a first glyph
        kerning.remove("A")
        assertThat(kerning.containsFirst("A")).isFalse()
        assertThat(kerning.pairCount).isEqualTo(1)
    }

    @Test
    fun testKerningWithGroups() {
        val kerning = KerningValues()

        // Kerning can reference group names
        kerning.set("public.kern1.A", "public.kern2.V", -50f)
        kerning.set("public.kern1.A", "o", -20f)
        kerning.set("T", "public.kern2.a", -30f)

        assertThat(kerning.get("public.kern1.A", "public.kern2.V")).isEqualTo(-50f)
        assertThat(kerning["public.kern1.A"]).containsEntry("public.kern2.V", -50f)
        assertThat(kerning["public.kern1.A"]).containsEntry("o", -20f)
    }

    @Test
    fun testForEach() {
        val kerning = KerningValues()
        kerning.set("A", "V", -50f)
        kerning.set("A", "W", -40f)
        kerning.set("T", "o", -30f)

        // Test forEach - collects all pairs
        val collectedPairs = mutableListOf<Triple<String, String, Float>>()
        kerning.forEach { first, second, value ->
            collectedPairs.add(Triple(first, second, value))
        }
        assertThat(collectedPairs).hasSize(3)
        assertThat(collectedPairs).contains(Triple("A", "V", -50f))
        assertThat(collectedPairs).contains(Triple("A", "W", -40f))
        assertThat(collectedPairs).contains(Triple("T", "o", -30f))

        // Test forEachFirst - collects by first glyph
        val collectedByFirst = mutableMapOf<String, Map<String, Float>>()
        kerning.forEachFirst { first, pairs ->
            collectedByFirst[first] = pairs
        }
        assertThat(collectedByFirst).hasSize(2)
        assertThat(collectedByFirst["A"]).containsExactly("V", -50f, "W", -40f)
        assertThat(collectedByFirst["T"]).containsExactly("o", -30f)
    }

    @Test
    fun testEmptyKerningNotWritten() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        val writer = UFOWriter(memPath)
        writer.writeKerning(KerningValues())

        // Verify kerning.plist was not created
        assertThat(Files.exists(memPath.resolve("kerning.plist"))).isFalse()
    }

    @Test
    fun testToKerningValuesExtension() {
        val map = mapOf(
            "A" to mapOf("V" to -50f, "W" to -40f),
            "T" to mapOf("o" to -30f)
        )

        val kerning = map.toKerningValues()

        assertThat(kerning.get("A", "V")).isEqualTo(-50f)
        assertThat(kerning.get("A", "W")).isEqualTo(-40f)
        assertThat(kerning.get("T", "o")).isEqualTo(-30f)
        assertThat(kerning.pairCount).isEqualTo(3)
    }

    private fun populateKerning(kerning: KerningValues) {
        kerning["a"] = mapOf("a" to 5f, "b" to -10f, "space" to 1f)
        kerning["b"] = mapOf("a" to -7f)
    }

    private fun verifyKerning(kerning: KerningValues) {
        assertThat(kerning.containsFirst("a")).isTrue()
        assertThat(kerning.containsFirst("b")).isTrue()
        assertThat(kerning.firstGlyphs).containsExactly("a", "b")

        assertThat(kerning.get("a", "a")).isEqualTo(5f)
        assertThat(kerning.get("a", "b")).isEqualTo(-10f)
        assertThat(kerning.get("a", "space")).isEqualTo(1f)
        assertThat(kerning.get("b", "a")).isEqualTo(-7f)

        assertThat(kerning["a"]).containsExactly("a", 5f, "b", -10f, "space", 1f)
        assertThat(kerning["b"]).containsExactly("a", -7f)

        assertThat(kerning.pairCount).isEqualTo(4)
    }
}

package io.github.adrientetar.ufo

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.writeText
import kotlin.test.Test

class LibTests {
    @Test
    fun testInvalidRead() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/path/to/TestFont.ufo")

        Files.createDirectories(memPath)
        val libPath = memPath.resolve("lib.plist")
        val reader = UFOReader(memPath)

        // When metainfo.plist is missing, pass as it's optional
        reader.readLib()

        // When metainfo.plist is empty, throw UFOLibException
        // cause: NullPointerException
        Files.createFile(libPath)
        assertThrows<UFOLibException> {
            reader.readLib()
        }

        // When metainfo.plist is invalid, throw UFOLibException
        // cause: SAXParseException
        libPath.writeText("<foo></bar>")
        assertThrows<UFOLibException> {
            reader.readLib()
        }
    }
    @Test
    fun testPopulateLib() {
        // Populate lib and verify (test the setters)
        val lib = LibValues()
        populateLib(lib)
        verifyLib(lib)
    }

    @Test
    fun testReadLib() {
        // Read from the sample font and verify (test the reader)
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val lib = reader.readLib()
        verifyLib(lib)
    }

    @Test
    fun testWriteLib() {
        // Set up an in-memory filesystem
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        run {
            // Read from the sample font
            val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
            val reader = UFOReader(ufo)
            val lib = reader.readLib()

            // Write to in-memory fs
            val writer = UFOWriter(memPath)
            writer.writeLib(lib)
        }

        // Read from in-memory fs and verify (test the writer)
        val reader = UFOReader(memPath)
        val lib = reader.readLib()
        verifyLib(lib)
    }

    private fun populateLib(lib: LibValues) {
        lib.glyphOrder = listOf(
            ".notdef", "glyph1", "glyph2", "space",
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"
        )
    }

    private fun verifyLib(lib: LibValues) {
        assertThat(lib.glyphOrder).isEqualTo(
            listOf(
                ".notdef", "glyph1", "glyph2", "space",
                "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"
            )
        )
    }
}

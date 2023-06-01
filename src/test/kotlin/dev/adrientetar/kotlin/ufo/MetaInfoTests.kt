package dev.adrientetar.kotlin.ufo

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import kotlin.test.Test

/**
 * Test [MetaInfoValues], including reading with [UFOReader] and writing with [UFOWriter].
 */
class MetaInfoTests {
    @Test
    fun testReadMetaInfo() {
        // Set up our sample font
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))

        // Read from the sample font and verify (test the reader)
        val reader = UFOReader(ufo)
        val meta = reader.readMetaInfo()

        assertThat(meta.creator).isEqualTo("org.robofab.ufoLib")
        assertThat(meta.formatVersion).isEqualTo(3)
    }

    @Test
    fun testWriteMetaInfo() {
        // Set up an in-memory filesystem
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        // Write to in-memory fs
        run {
            val writer = UFOWriter(memPath)
            writer.writeMetaInfo()
        }

        // Read from in-memory fs and verify (test the writer)
        val reader = UFOReader(memPath)
        val meta = reader.readMetaInfo()

        assertThat(meta.creator).isEqualTo("dev.adrientetar.kotlin.ufo")
        assertThat(meta.formatVersion).isEqualTo(3)
    }
}

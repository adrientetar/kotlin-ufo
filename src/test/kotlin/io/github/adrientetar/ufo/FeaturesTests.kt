package io.github.adrientetar.ufo

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test

class FeaturesTests {
    @Test
    fun testEmptyFeatures() {
        val features = FeaturesValues()
        assertThat(features.isEmpty).isTrue()
        assertThat(features.textOrEmpty()).isEqualTo("")
    }

    @Test
    fun testPopulateFeatures() {
        val features = FeaturesValues()
        features.text = SAMPLE_FEATURES
        assertThat(features.isEmpty).isFalse()
        assertThat(features.text).isEqualTo(SAMPLE_FEATURES)
    }

    @Test
    fun testReadFeatures() {
        // Read from the sample font and verify (test the reader)
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val features = reader.readFeatures()

        assertThat(features.isEmpty).isFalse()
        assertThat(features.text).contains("languagesystem DFLT dflt")
        assertThat(features.text).contains("feature kern")
        assertThat(features.text).contains("feature liga")
    }

    @Test
    fun testReadMissingFeatures() {
        // Set up an in-memory filesystem without features.fea
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")
        Files.createDirectories(memPath)

        val reader = UFOReader(memPath)
        val features = reader.readFeatures()

        assertThat(features.isEmpty).isTrue()
        assertThat(features.text).isNull()
    }

    @Test
    fun testWriteFeatures() {
        // Set up an in-memory filesystem
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        run {
            // Read from the sample font
            val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
            val reader = UFOReader(ufo)
            val features = reader.readFeatures()

            // Write to in-memory fs
            val writer = UFOWriter(memPath)
            writer.writeFeatures(features)
        }

        // Read from in-memory fs and verify (test the writer)
        val reader = UFOReader(memPath)
        val features = reader.readFeatures()

        assertThat(features.isEmpty).isFalse()
        assertThat(features.text).contains("languagesystem DFLT dflt")
        assertThat(features.text).contains("feature kern")
        assertThat(features.text).contains("feature liga")
    }

    @Test
    fun testWriteEmptyFeatures() {
        // Set up an in-memory filesystem
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        run {
            val writer = UFOWriter(memPath)
            writer.writeFeatures(FeaturesValues()) // Empty features
        }

        // Verify the file was not created
        val featuresPath = memPath.resolve("features.fea")
        assertThat(Files.exists(featuresPath)).isFalse()
    }

    @Test
    fun testWriteEmptyFeaturesDeletesExisting() {
        // Set up an in-memory filesystem with a pre-existing features.fea
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")
        Files.createDirectories(memPath)
        val featuresPath = memPath.resolve("features.fea")
        Files.writeString(featuresPath, "feature kern { } kern;")

        // Verify the file exists
        assertThat(Files.exists(featuresPath)).isTrue()

        // Write empty features (without clearing directory)
        val writer = UFOWriter(memPath, clearDirectory = false)
        writer.writeFeatures(FeaturesValues())

        // Verify the file was deleted
        assertThat(Files.exists(featuresPath)).isFalse()
    }

    @Test
    fun testFeaturesEquality() {
        val features1 = FeaturesValues(SAMPLE_FEATURES)
        val features2 = FeaturesValues(SAMPLE_FEATURES)
        val features3 = FeaturesValues("different content")

        assertThat(features1).isEqualTo(features2)
        assertThat(features1.hashCode()).isEqualTo(features2.hashCode())
        assertThat(features1).isNotEqualTo(features3)
    }

    companion object {
        private const val SAMPLE_FEATURES = """
languagesystem DFLT dflt;
languagesystem latn dflt;

feature kern {
    pos a b -50;
} kern;
"""
    }
}

package io.github.adrientetar.ufo

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test

class ImagesDirectoryTests {
    @Test
    fun testImagesExists() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val images = reader.images()

        assertThat(images.exists).isTrue()
    }

    @Test
    fun testListImages() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val images = reader.images()

        val imageList = images.listImages()
        assertThat(imageList).contains("test.png")
    }

    @Test
    fun testHasImage() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val images = reader.images()

        assertThat(images.hasImage("test.png")).isTrue()
        assertThat(images.hasImage("nonexistent.png")).isFalse()
    }

    @Test
    fun testReadImage() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val images = reader.images()

        val imageData = images.readImage("test.png")
        // PNG magic number
        assertThat(imageData.size).isGreaterThan(0)
        assertThat(imageData[0]).isEqualTo(0x89.toByte())
        assertThat(imageData[1]).isEqualTo('P'.code.toByte())
        assertThat(imageData[2]).isEqualTo('N'.code.toByte())
        assertThat(imageData[3]).isEqualTo('G'.code.toByte())
    }

    @Test
    fun testReadImageOrNull() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val images = reader.images()

        assertThat(images.readImageOrNull("test.png")).isNotNull()
        assertThat(images.readImageOrNull("nonexistent.png")).isNull()
    }

    @Test
    fun testWriteImage() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")
        Files.createDirectories(memPath)

        val writer = UFOWriter(memPath, clearDirectory = false)
        val images = writer.images()

        // Create a minimal PNG-like data (just for testing, not a real PNG)
        val testData = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())
        images.writeImage("new_image.png", testData)

        assertThat(images.hasImage("new_image.png")).isTrue()
        assertThat(images.readImage("new_image.png")).isEqualTo(testData)
    }

    @Test
    fun testDeleteImage() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")
        Files.createDirectories(memPath)

        val writer = UFOWriter(memPath, clearDirectory = false)
        val images = writer.images()

        // Write then delete
        val testData = byteArrayOf(1, 2, 3, 4)
        images.writeImage("to_delete.png", testData)
        assertThat(images.hasImage("to_delete.png")).isTrue()

        val deleted = images.deleteImage("to_delete.png")
        assertThat(deleted).isTrue()
        assertThat(images.hasImage("to_delete.png")).isFalse()

        // Deleting non-existent returns false
        assertThat(images.deleteImage("nonexistent.png")).isFalse()
    }

    @Test
    fun testCopyImages() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        // Read from sample font
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val sourceReader = UFOReader(ufo)
        val sourceImages = sourceReader.images()

        // Copy to in-memory fs
        val writer = UFOWriter(memPath)
        val targetImages = writer.images()
        targetImages.copyFrom(sourceImages)

        // Verify
        assertThat(targetImages.listImages()).contains("test.png")
        assertThat(targetImages.readImage("test.png")).isEqualTo(sourceImages.readImage("test.png"))
    }

    @Test
    fun testCopyFromEmptySource() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        val writer = UFOWriter(memPath)
        val targetImages = writer.images()

        val sourceImages = ImagesDirectory(fs.getPath("/nonexistent/images"))
        targetImages.copyFrom(sourceImages)
        assertThat(targetImages.exists).isFalse()
    }

    @Test
    fun testEmptyImagesDirectory() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")
        Files.createDirectories(memPath)

        val reader = UFOReader(memPath)
        val images = reader.images()

        assertThat(images.exists).isFalse()
        assertThat(images.listImages()).isEmpty()
    }
}

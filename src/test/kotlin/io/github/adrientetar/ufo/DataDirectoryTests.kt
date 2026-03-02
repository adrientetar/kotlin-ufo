package io.github.adrientetar.ufo

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test

class DataDirectoryTests {
    @Test
    fun testDataExists() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val data = reader.data()

        assertThat(data.exists).isTrue()
    }

    @Test
    fun testListEntries() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val data = reader.data()

        val entries = data.listEntries()
        assertThat(entries).contains("com.github.fonttools.ttx")
    }

    @Test
    fun testHasEntry() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val data = reader.data()

        assertThat(data.hasEntry("com.github.fonttools.ttx")).isTrue()
        assertThat(data.hasEntry("nonexistent")).isFalse()
    }

    @Test
    fun testIsDirectory() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val data = reader.data()

        assertThat(data.isDirectory("com.github.fonttools.ttx")).isTrue()
    }

    @Test
    fun testReadFile() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val data = reader.data()

        val content = data.readFileAsString("com.github.fonttools.ttx/CUST.ttx")
        assertThat(content).isNotEmpty()
    }

    @Test
    fun testReadFileOrNull() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val data = reader.data()

        assertThat(data.readFileOrNull("com.github.fonttools.ttx/CUST.ttx")).isNotNull()
        assertThat(data.readFileOrNull("nonexistent/file.txt")).isNull()
    }

    @Test
    fun testListFilesInDirectory() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val data = reader.data()

        val files = data.listFilesInDirectory("com.github.fonttools.ttx")
        assertThat(files).contains("com.github.fonttools.ttx/CUST.ttx")
    }

    @Test
    fun testWriteFile() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")
        Files.createDirectories(memPath)

        val writer = UFOWriter(memPath, clearDirectory = false)
        val data = writer.data()

        data.writeFileAsString("com.example.test/config.json", """{"key": "value"}""")

        assertThat(data.hasEntry("com.example.test")).isTrue()
        assertThat(data.isDirectory("com.example.test")).isTrue()
        assertThat(data.readFileAsString("com.example.test/config.json")).isEqualTo("""{"key": "value"}""")
    }

    @Test
    fun testWriteFileBytes() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")
        Files.createDirectories(memPath)

        val writer = UFOWriter(memPath, clearDirectory = false)
        val data = writer.data()

        val testData = byteArrayOf(1, 2, 3, 4, 5)
        data.writeFile("com.example.binary/data.bin", testData)

        assertThat(data.readFile("com.example.binary/data.bin")).isEqualTo(testData)
    }

    @Test
    fun testDelete() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")
        Files.createDirectories(memPath)

        val writer = UFOWriter(memPath, clearDirectory = false)
        val data = writer.data()

        // Create a nested structure
        data.writeFileAsString("com.example.test/subdir/file1.txt", "content1")
        data.writeFileAsString("com.example.test/subdir/file2.txt", "content2")
        assertThat(data.hasEntry("com.example.test")).isTrue()

        // Delete the entire entry
        val deleted = data.delete("com.example.test")
        assertThat(deleted).isTrue()
        assertThat(data.hasEntry("com.example.test")).isFalse()

        // Deleting non-existent returns false
        assertThat(data.delete("nonexistent")).isFalse()
    }

    @Test
    fun testCopyEntryFrom() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        // Read from sample font
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val sourceReader = UFOReader(ufo)
        val sourceData = sourceReader.data()

        // Copy to in-memory fs
        val writer = UFOWriter(memPath)
        val targetData = writer.data()
        targetData.copyEntryFrom(sourceData, "com.github.fonttools.ttx")

        // Verify
        assertThat(targetData.hasEntry("com.github.fonttools.ttx")).isTrue()
        assertThat(targetData.isDirectory("com.github.fonttools.ttx")).isTrue()
        assertThat(targetData.readFileAsString("com.github.fonttools.ttx/CUST.ttx"))
            .isEqualTo(sourceData.readFileAsString("com.github.fonttools.ttx/CUST.ttx"))
    }

    @Test
    fun testCopyFrom() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        // Read from sample font
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val sourceReader = UFOReader(ufo)
        val sourceData = sourceReader.data()

        // Copy all entries to in-memory fs
        val writer = UFOWriter(memPath)
        val targetData = writer.data()
        targetData.copyFrom(sourceData)

        // Verify all entries copied
        assertThat(targetData.listEntries()).containsExactlyElementsIn(sourceData.listEntries())
    }

    @Test
    fun testReadFileAsStringOrNull() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val data = reader.data()

        assertThat(data.readFileAsStringOrNull("com.github.fonttools.ttx/CUST.ttx")).isNotNull()
        assertThat(data.readFileAsStringOrNull("nonexistent/file.txt")).isNull()
    }

    @Test
    fun testIsFileAndDirectoryEdgeCases() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val data = reader.data()

        assertThat(data.isFile("com.github.fonttools.ttx/CUST.ttx")).isTrue()
        assertThat(data.isFile("com.github.fonttools.ttx")).isFalse()
        assertThat(data.isDirectory("nonexistent")).isFalse()
        assertThat(data.isFile("nonexistent")).isFalse()
    }

    @Test
    fun testDeleteSingleFile() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")
        Files.createDirectories(memPath)

        val writer = UFOWriter(memPath, clearDirectory = false)
        val data = writer.data()

        data.writeFileAsString("com.example.simple.txt", "content")
        assertThat(data.isFile("com.example.simple.txt")).isTrue()

        val deleted = data.delete("com.example.simple.txt")
        assertThat(deleted).isTrue()
        assertThat(data.hasEntry("com.example.simple.txt")).isFalse()
    }

    @Test
    fun testListFilesInNonexistentDirectory() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")
        Files.createDirectories(memPath)

        val reader = UFOReader(memPath)
        val data = reader.data()
        assertThat(data.listFilesInDirectory("nonexistent")).isEmpty()
    }

    @Test
    fun testCopyEntryFromNonexistent() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        val writer = UFOWriter(memPath)
        val targetData = writer.data()

        val sourceData = DataDirectory(fs.getPath("/nonexistent/data"))
        // Should be a no-op, not throw
        targetData.copyEntryFrom(sourceData, "nonexistent")
        assertThat(targetData.exists).isFalse()
    }

    @Test
    fun testCopyFromEmptySource() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        val writer = UFOWriter(memPath)
        val targetData = writer.data()

        val sourceData = DataDirectory(fs.getPath("/nonexistent/data"))
        targetData.copyFrom(sourceData)
        assertThat(targetData.exists).isFalse()
    }

    @Test
    fun testEmptyDataDirectory() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")
        Files.createDirectories(memPath)

        val reader = UFOReader(memPath)
        val data = reader.data()

        assertThat(data.exists).isFalse()
        assertThat(data.listEntries()).isEmpty()
    }

    @Test
    fun testWriteTopLevelFile() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")
        Files.createDirectories(memPath)

        val writer = UFOWriter(memPath, clearDirectory = false)
        val data = writer.data()

        // Write a top-level file (not in a subdirectory)
        data.writeFileAsString("com.example.simple.txt", "simple content")

        assertThat(data.hasEntry("com.example.simple.txt")).isTrue()
        assertThat(data.isFile("com.example.simple.txt")).isTrue()
        assertThat(data.readFileAsString("com.example.simple.txt")).isEqualTo("simple content")
    }
}

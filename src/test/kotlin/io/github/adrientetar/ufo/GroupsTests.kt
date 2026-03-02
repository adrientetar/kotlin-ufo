package io.github.adrientetar.ufo

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.writeText
import kotlin.test.Test

class GroupsTests {
    @Test
    fun testInvalidRead() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/path/to/TestFont.ufo")

        Files.createDirectories(memPath)
        val groupsPath = memPath.resolve("groups.plist")
        val reader = UFOReader(memPath)

        // When groups.plist is missing, pass as it's optional
        val emptyGroups = reader.readGroups()
        assertThat(emptyGroups.groupNames).isEmpty()

        // When groups.plist is empty, throw UFOLibException
        // cause: NullPointerException
        Files.createFile(groupsPath)
        assertThrows<UFOLibException> {
            reader.readGroups()
        }

        // When groups.plist is invalid, throw UFOLibException
        // cause: SAXParseException
        groupsPath.writeText("<foo></bar>")
        assertThrows<UFOLibException> {
            reader.readGroups()
        }
    }

    @Test
    fun testPopulateGroups() {
        // Populate groups and verify (test the setters)
        val groups = GroupsValues()
        populateGroups(groups)
        verifyGroups(groups)
    }

    @Test
    fun testReadGroups() {
        // Read from the sample font and verify (test the reader)
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val groups = reader.readGroups()
        verifyGroups(groups)
    }

    @Test
    fun testWriteGroups() {
        // Set up an in-memory filesystem
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        run {
            // Read from the sample font
            val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
            val reader = UFOReader(ufo)
            val groups = reader.readGroups()

            // Write to in-memory fs
            val writer = UFOWriter(memPath)
            writer.writeGroups(groups)
        }

        // Read from in-memory fs and verify (test the writer)
        val reader = UFOReader(memPath)
        val groups = reader.readGroups()
        verifyGroups(groups)
    }

    @Test
    fun testKerningGroupFilters() {
        val groups = GroupsValues()
        groups["public.kern1.A"] = listOf("A", "Agrave", "Aacute")
        groups["public.kern1.O"] = listOf("O", "Ograve")
        groups["public.kern2.A"] = listOf("A", "Agrave")
        groups["myCustomGroup"] = listOf("a", "b", "c")

        // Test forEachFirstKerningGroup
        val firstGroupNames = mutableListOf<String>()
        val firstGroupGlyphs = mutableMapOf<String, List<String>>()
        groups.forEachFirstKerningGroup { name, glyphs ->
            firstGroupNames.add(name)
            firstGroupGlyphs[name] = glyphs
        }
        assertThat(firstGroupNames).containsExactly("public.kern1.A", "public.kern1.O")
        assertThat(firstGroupGlyphs["public.kern1.A"]).containsExactly("A", "Agrave", "Aacute")

        // Test forEachSecondKerningGroup
        val secondGroupNames = mutableListOf<String>()
        val secondGroupGlyphs = mutableMapOf<String, List<String>>()
        groups.forEachSecondKerningGroup { name, glyphs ->
            secondGroupNames.add(name)
            secondGroupGlyphs[name] = glyphs
        }
        assertThat(secondGroupNames).containsExactly("public.kern2.A")
        assertThat(secondGroupGlyphs["public.kern2.A"]).containsExactly("A", "Agrave")
    }

    @Test
    fun testEmptyGroupsNotWritten() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        val writer = UFOWriter(memPath)
        writer.writeGroups(GroupsValues())

        // Verify groups.plist was not created
        assertThat(Files.exists(memPath.resolve("groups.plist"))).isFalse()
    }

    @Test
    fun testForEach() {
        val groups = GroupsValues()
        groups["public.kern1.A"] = listOf("A", "Agrave")
        groups["stems"] = listOf("b", "d")

        val collected = mutableMapOf<String, List<String>>()
        groups.forEach { name, glyphs ->
            collected[name] = glyphs
        }
        assertThat(collected).hasSize(2)
        assertThat(collected["public.kern1.A"]).containsExactly("A", "Agrave")
        assertThat(collected["stems"]).containsExactly("b", "d")
    }

    @Test
    fun testIterator() {
        val groups = GroupsValues()
        groups["groupA"] = listOf("a", "b")
        groups["groupB"] = listOf("c")

        val entries = mutableMapOf<String, List<String>>()
        for (entry in groups) {
            entries[entry.key] = entry.value
        }
        assertThat(entries).hasSize(2)
        assertThat(entries["groupA"]).containsExactly("a", "b")
        assertThat(entries["groupB"]).containsExactly("c")
    }

    @Test
    fun testRemoveGroup() {
        val groups = GroupsValues()
        groups["toRemove"] = listOf("a", "b")
        groups["toKeep"] = listOf("c")

        assertThat(groups.containsGroup("toRemove")).isTrue()
        groups.remove("toRemove")
        assertThat(groups.containsGroup("toRemove")).isFalse()
        assertThat(groups["toRemove"]).isNull()
        assertThat(groups.containsGroup("toKeep")).isTrue()
    }

    @Test
    fun testSetGroupToNull() {
        val groups = GroupsValues()
        groups["myGroup"] = listOf("a", "b")
        assertThat(groups.containsGroup("myGroup")).isTrue()

        groups["myGroup"] = null
        assertThat(groups.containsGroup("myGroup")).isFalse()
        assertThat(groups["myGroup"]).isNull()
    }

    @Test
    fun testGetNonexistentGroup() {
        val groups = GroupsValues()
        assertThat(groups["nonexistent"]).isNull()
        assertThat(groups.containsGroup("nonexistent")).isFalse()
    }

    @Test
    fun testEmptyGroupNames() {
        val groups = GroupsValues()
        assertThat(groups.groupNames).isEmpty()
    }

    @Test
    fun testKerningGroupFiltersEmpty() {
        val groups = GroupsValues()
        groups["customGroup"] = listOf("a")

        val firstGroups = mutableListOf<String>()
        groups.forEachFirstKerningGroup { name, _ -> firstGroups.add(name) }
        assertThat(firstGroups).isEmpty()

        val secondGroups = mutableListOf<String>()
        groups.forEachSecondKerningGroup { name, _ -> secondGroups.add(name) }
        assertThat(secondGroups).isEmpty()
    }

    private fun populateGroups(groups: GroupsValues) {
        groups["public.kern1.A"] = listOf("A", "Agrave", "Aacute")
        groups["public.kern2.V"] = listOf("V", "W")
        groups["stems"] = listOf("b", "d", "h", "k", "l")
    }

    private fun verifyGroups(groups: GroupsValues) {
        assertThat(groups.containsGroup("public.kern1.A")).isTrue()
        assertThat(groups["public.kern1.A"]).containsExactly("A", "Agrave", "Aacute")
        assertThat(groups["public.kern2.V"]).containsExactly("V", "W")
        assertThat(groups["stems"]).containsExactly("b", "d", "h", "k", "l")
        assertThat(groups.groupNames).containsExactly("public.kern1.A", "public.kern2.V", "stems")
    }
}

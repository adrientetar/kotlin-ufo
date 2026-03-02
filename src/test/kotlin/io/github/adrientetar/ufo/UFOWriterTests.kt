package io.github.adrientetar.ufo

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test

class UFOWriterTests {
    @Test
    fun testGLIFFileName() {
        assertThat("a".toFileName()).isEqualTo("a")
        assertThat("A".toFileName()).isEqualTo("A_")
        assertThat("AE".toFileName()).isEqualTo("A_E_")
        assertThat("Ae".toFileName()).isEqualTo("A_e")
        assertThat("ae".toFileName()).isEqualTo("ae")
        assertThat("aE".toFileName()).isEqualTo("aE_")
        assertThat("a.alt".toFileName()).isEqualTo("a.alt")
        assertThat("A.alt".toFileName()).isEqualTo("A_.alt")
        assertThat("A.Alt".toFileName()).isEqualTo("A_.A_lt")
        assertThat("A.aLt".toFileName()).isEqualTo("A_.aL_t")
        assertThat("A.alT".toFileName()).isEqualTo("A_.alT_")
        assertThat("T_H".toFileName()).isEqualTo("T__H_")
        assertThat("T_h".toFileName()).isEqualTo("T__h")
        assertThat("t_h".toFileName()).isEqualTo("t_h")
        assertThat("F_F_I".toFileName()).isEqualTo("F__F__I_")
        assertThat("f_f_i".toFileName()).isEqualTo("f_f_i")
        assertThat("Aacute_V.swash".toFileName()).isEqualTo("A_acute_V_.swash")
        assertThat(".notdef".toFileName()).isEqualTo("_notdef")
        assertThat("con".toFileName()).isEqualTo("_con")
        assertThat("CON".toFileName()).isEqualTo("C_O_N_")
        assertThat("con.alt".toFileName()).isEqualTo("_con.alt")
        assertThat("alt.con".toFileName()).isEqualTo("alt._con")
    }

    @Test
    fun testGLIFFileNameCollisionAvoidance() {
        // No collision with empty set
        assertThat("a".toFileName(emptySet())).isEqualTo("a")

        // No collision when name is not in existing set
        assertThat("a".toFileName(setOf("b"))).isEqualTo("a")

        // Collision detected — append 15-digit counter
        val existing = setOf("a")
        assertThat("a".toFileName(existing)).isEqualTo("a" + "000000000000001")

        // Case-insensitive collision
        val existingUpper = setOf("a_")
        assertThat("A".toFileName(existingUpper)).isEqualTo("A_" + "000000000000001")
    }

    @Test
    fun testGLIFFileNameMultipleCollisions() {
        // Multiple collisions increment the counter
        val existing = mutableSetOf("a", "a000000000000001")
        assertThat("a".toFileName(existing)).isEqualTo("a" + "000000000000002")
    }

    @Test
    fun testWriteGlyphsWithCollision() {
        // Two glyph names that map to the same filename
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        // "A" -> "A_" and "A+" -> "A_" (+ is illegal, replaced with _)
        // These would collide without collision avoidance
        val glyphA = GlyphValues().apply {
            name = "A"
            width = 500f
        }
        val glyphAPlus = GlyphValues().apply {
            name = "A+"
            width = 600f
        }

        val writer = UFOWriter(memPath)
        writer.writeMetaInfo()
        writer.writeGlyphs(listOf(glyphA, glyphAPlus))

        // Read back and verify both glyphs survived
        val reader = UFOReader(memPath)
        val glyphs = reader.readGlyphs().toList()
        assertThat(glyphs).hasSize(2)

        val names = glyphs.map { it.name }.toSet()
        assertThat(names).containsExactly("A", "A+")

        val widths = glyphs.associate { it.name to it.width }
        assertThat(widths["A"]).isEqualTo(500f)
        assertThat(widths["A+"]).isEqualTo(600f)
    }

    @Test
    fun testOverwriteExistingFile() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/path/to/TestFont.ufo")

        Files.createDirectories(memPath.parent)
        Files.createFile(memPath)

        val writer = UFOWriter(memPath)
        writer.writeMetaInfo()

        readAndVerifyMetaInfo(memPath)
    }

    @Test
    fun testOverwriteExistingFolder() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/path/to/TestFont.ufo")

        Files.createDirectories(memPath)
        Files.createFile(memPath.resolve("abc.123"))

        val writer = UFOWriter(memPath)
        writer.writeMetaInfo()

        readAndVerifyMetaInfo(memPath)
    }

    private fun readAndVerifyMetaInfo(ufo: Path) {
        val reader = UFOReader(ufo)
        val meta = reader.readMetaInfo()

        assertThat(meta.creator).isEqualTo("dev.adrientetar.kotlin.ufo")
        assertThat(meta.formatVersion).isEqualTo(3)
    }

    @Test
    fun `serializeOutline preserves interleaved order`() {
        val outline = Outline()
        outline.elements.addAll(listOf(
            Contour(points = listOf(Point(0f, 0f, "line"))),
            Component(base = "A", xOffset = 10f),
            Contour(identifier = "c2", points = listOf(Point(100f, 200f, "curve", "yes"))),
            Component(base = "acute"),
        ))

        val xml = UFOWriter.serializeOutline(outline)!!
        assertThat(xml).contains("<contour>")
        assertThat(xml).contains("<component base=\"A\"")

        // Verify ordering: first contour before component A, component A before second contour
        val contour1Pos = xml.indexOf("<contour>")
        val componentAPos = xml.indexOf("<component base=\"A\"")
        val contour2Pos = xml.indexOf("<contour identifier=\"c2\">")
        val componentAcutePos = xml.indexOf("<component base=\"acute\"")

        assertThat(contour1Pos).isLessThan(componentAPos)
        assertThat(componentAPos).isLessThan(contour2Pos)
        assertThat(contour2Pos).isLessThan(componentAcutePos)
    }

    @Test
    fun `serializeOutline returns null for empty outline`() {
        val outline = Outline()
        assertThat(UFOWriter.serializeOutline(outline)).isNull()
    }

    @Test
    fun `serializeOutline writes component attributes`() {
        val outline = Outline()
        outline.elements.add(Component(
            base = "A",
            xScale = 1f,
            xyScale = 0f,
            yxScale = 0f,
            yScale = 1f,
            xOffset = 10f,
            yOffset = 20f,
            identifier = "comp1"
        ))

        val xml = UFOWriter.serializeOutline(outline)!!
        assertThat(xml).contains("base=\"A\"")
        assertThat(xml).contains("xScale=\"1\"")
        assertThat(xml).contains("xOffset=\"10\"")
        assertThat(xml).contains("yOffset=\"20\"")
        assertThat(xml).contains("identifier=\"comp1\"")
    }

    @Test
    fun `serializeOutline writes point smooth attribute`() {
        val outline = Outline()
        outline.elements.add(Contour(points = listOf(
            Point(100f, 200f, "curve", "yes"),
        )))

        val xml = UFOWriter.serializeOutline(outline)!!
        assertThat(xml).contains("smooth=\"yes\"")
    }
}

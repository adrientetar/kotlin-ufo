package dev.adrientetar.kotlin.ufo

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
}

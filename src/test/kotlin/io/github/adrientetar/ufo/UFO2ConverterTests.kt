package io.github.adrientetar.ufo

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test

/**
 * Unit tests for [UFO2Converter].
 */
class UFO2ConverterTests {

    // -- convertGlif --

    @Test
    fun `convertGlif extracts single-point move contour as anchor`() {
        val glif = Glif().apply {
            format = 1
            outline.contours = listOf(
                Contour(points = listOf(
                    Point(x = 100f, y = 200f, type = "line"),
                    Point(x = 300f, y = 400f, type = "line")
                )),
                Contour(points = listOf(
                    Point(x = 250f, y = 650f, type = "move", name = "top")
                ))
            )
        }

        UFO2Converter.convertGlif(glif)

        assertThat(glif.format).isEqualTo(2)
        assertThat(glif.outline.contours).hasSize(1)
        assertThat(glif.outline.contours!![0].points).hasSize(2)
        assertThat(glif.anchors).hasSize(1)
        assertThat(glif.anchors!![0]).isEqualTo(Anchor(x = 250f, y = 650f, name = "top"))
    }

    @Test
    fun `convertGlif extracts multiple anchor contours`() {
        val glif = Glif().apply {
            format = 1
            outline.contours = listOf(
                Contour(points = listOf(
                    Point(x = 250f, y = 650f, type = "move", name = "top")
                )),
                Contour(points = listOf(
                    Point(x = 0f, y = 0f, type = "move", name = "bottom")
                ))
            )
        }

        UFO2Converter.convertGlif(glif)

        assertThat(glif.format).isEqualTo(2)
        assertThat(glif.outline.contours).isNull()
        assertThat(glif.anchors).hasSize(2)
        assertThat(glif.anchors!![0].name).isEqualTo("top")
        assertThat(glif.anchors!![1].name).isEqualTo("bottom")
    }

    @Test
    fun `convertGlif handles anchor without name`() {
        val glif = Glif().apply {
            format = 1
            outline.contours = listOf(
                Contour(points = listOf(
                    Point(x = 100f, y = 200f, type = "move")
                ))
            )
        }

        UFO2Converter.convertGlif(glif)

        assertThat(glif.format).isEqualTo(2)
        assertThat(glif.anchors).hasSize(1)
        assertThat(glif.anchors!![0]).isEqualTo(Anchor(x = 100f, y = 200f, name = null))
    }

    @Test
    fun `convertGlif preserves multi-point contours`() {
        val glif = Glif().apply {
            format = 1
            outline.contours = listOf(
                Contour(points = listOf(
                    Point(x = 0f, y = 0f, type = "line"),
                    Point(x = 100f, y = 0f, type = "line"),
                    Point(x = 50f, y = 100f, type = "line")
                ))
            )
        }

        UFO2Converter.convertGlif(glif)

        assertThat(glif.format).isEqualTo(2)
        assertThat(glif.outline.contours).hasSize(1)
        assertThat(glif.anchors).isNull()
    }

    @Test
    fun `convertGlif skips format 2 glifs`() {
        val glif = Glif().apply {
            format = 2
            outline.contours = listOf(
                Contour(points = listOf(
                    Point(x = 250f, y = 650f, type = "move", name = "top")
                ))
            )
        }

        UFO2Converter.convertGlif(glif)

        // Should not have been converted — contour still there
        assertThat(glif.format).isEqualTo(2)
        assertThat(glif.outline.contours).hasSize(1)
        assertThat(glif.anchors).isNull()
    }

    @Test
    fun `convertGlif handles empty contours`() {
        val glif = Glif().apply {
            format = 1
            outline.contours = null
        }

        UFO2Converter.convertGlif(glif)

        assertThat(glif.format).isEqualTo(2)
        assertThat(glif.anchors).isNull()
    }

    // -- convertKerningAndGroups --

    @Test
    fun `convertKerningAndGroups renames MMK groups`() {
        val groups = GroupsValues().apply {
            dict.put("@MMK_L_A", arrayOf("a"))
            dict.put("@MMK_R_V", arrayOf("space"))
            dict.put("stems", arrayOf("b", "d"))
        }
        val kerning = KerningValues().apply {
            val seconds = com.dd.plist.NSDictionary()
            seconds.put("@MMK_R_V", -50)
            dict.put("@MMK_L_A", seconds)
        }

        UFO2Converter.convertKerningAndGroups(kerning, groups)

        // New groups should be added
        assertThat(groups.dict.containsKey("public.kern1.A")).isTrue()
        assertThat(groups.dict.containsKey("public.kern2.V")).isTrue()
        // Old groups are preserved
        assertThat(groups.dict.containsKey("@MMK_L_A")).isTrue()
        assertThat(groups.dict.containsKey("@MMK_R_V")).isTrue()
        // Non-kerning groups are untouched
        assertThat(groups.dict.containsKey("stems")).isTrue()

        // Kerning references should use new names
        assertThat(kerning.dict.containsKey("public.kern1.A")).isTrue()
        val seconds = kerning.dict["public.kern1.A"]!!.toDictionary()
        assertThat(seconds.containsKey("public.kern2.V")).isTrue()
    }

    @Test
    fun `convertKerningAndGroups handles no MMK groups`() {
        val groups = GroupsValues().apply {
            dict.put("stems", arrayOf("b", "d"))
        }
        val kerning = KerningValues().apply {
            val seconds = com.dd.plist.NSDictionary()
            seconds.put("space", 5)
            dict.put("a", seconds)
        }

        UFO2Converter.convertKerningAndGroups(kerning, groups)

        // Nothing should change
        assertThat(groups.dict.allKeys().toSet()).containsExactly("stems")
        assertThat(kerning.dict.containsKey("a")).isTrue()
    }
}

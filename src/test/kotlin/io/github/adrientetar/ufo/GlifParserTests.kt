package io.github.adrientetar.ufo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GlifParserTests {

    @Test
    fun `parse simple glyph with advance and unicode`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <glyph name="a" format="2">
              <advance height="750" width="388"/>
              <unicode hex="0061"/>
            </glyph>
        """.trimIndent()

        val glif = GlifParser.parse(xml)

        assertEquals("a", glif.name)
        assertEquals(2, glif.format)
        assertEquals(388f, glif.advance.width)
        assertEquals(750f, glif.advance.height)
        assertNotNull(glif.unicodes)
        assertEquals(1, glif.unicodes!!.size)
        assertEquals("0061", glif.unicodes!![0].hex)
    }

    @Test
    fun `parse glyph with multiple unicodes`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <glyph name="omega" format="2">
              <advance width="500"/>
              <unicode hex="03C9"/>
              <unicode hex="2126"/>
            </glyph>
        """.trimIndent()

        val glif = GlifParser.parse(xml)

        assertEquals("omega", glif.name)
        assertEquals(2, glif.unicodes!!.size)
        assertEquals("03C9", glif.unicodes!![0].hex)
        assertEquals("2126", glif.unicodes!![1].hex)
    }

    @Test
    fun `parse glyph with note`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <glyph name="test" format="2">
              <note>This is a test note</note>
            </glyph>
        """.trimIndent()

        val glif = GlifParser.parse(xml)

        assertEquals("This is a test note", glif.note)
    }

    @Test
    fun `parse glyph with image`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <glyph name="test" format="2">
              <image fileName="test.png" xScale="0.5" yScale="0.5" xOffset="10" yOffset="20"/>
            </glyph>
        """.trimIndent()

        val glif = GlifParser.parse(xml)

        assertNotNull(glif.image)
        assertEquals("test.png", glif.image!!.fileName)
        assertEquals(0.5f, glif.image!!.xScale)
        assertEquals(0.5f, glif.image!!.yScale)
        assertEquals(10f, glif.image!!.xOffset)
        assertEquals(20f, glif.image!!.yOffset)
    }

    @Test
    fun `parse glyph with guidelines`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <glyph name="test" format="2">
              <guideline x="100" name="vertical"/>
              <guideline y="500" name="horizontal"/>
              <guideline x="200" y="300" angle="45" color="1,0,0,1" identifier="guide1"/>
            </glyph>
        """.trimIndent()

        val glif = GlifParser.parse(xml)

        assertNotNull(glif.guidelines)
        assertEquals(3, glif.guidelines!!.size)

        val g1 = glif.guidelines!![0]
        assertEquals(100f, g1.x)
        assertNull(g1.y)
        assertEquals("vertical", g1.name)

        val g2 = glif.guidelines!![1]
        assertNull(g2.x)
        assertEquals(500f, g2.y)
        assertEquals("horizontal", g2.name)

        val g3 = glif.guidelines!![2]
        assertEquals(200f, g3.x)
        assertEquals(300f, g3.y)
        assertEquals(45f, g3.angle)
        assertEquals("1,0,0,1", g3.color)
        assertEquals("guide1", g3.identifier)
    }

    @Test
    fun `parse glyph with anchors`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <glyph name="test" format="2">
              <anchor x="250" y="550" name="top"/>
              <anchor x="250" y="0" name="bottom" color="0,1,0,1" identifier="anchor1"/>
            </glyph>
        """.trimIndent()

        val glif = GlifParser.parse(xml)

        assertNotNull(glif.anchors)
        assertEquals(2, glif.anchors!!.size)

        val a1 = glif.anchors!![0]
        assertEquals(250f, a1.x)
        assertEquals(550f, a1.y)
        assertEquals("top", a1.name)

        val a2 = glif.anchors!![1]
        assertEquals(250f, a2.x)
        assertEquals(0f, a2.y)
        assertEquals("bottom", a2.name)
        assertEquals("0,1,0,1", a2.color)
        assertEquals("anchor1", a2.identifier)
    }

    @Test
    fun `parse glyph with contours`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <glyph name="square" format="2">
              <outline>
                <contour>
                  <point x="0" y="0" type="line"/>
                  <point x="100" y="0" type="line"/>
                  <point x="100" y="100" type="line"/>
                  <point x="0" y="100" type="line"/>
                </contour>
              </outline>
            </glyph>
        """.trimIndent()

        val glif = GlifParser.parse(xml)

        assertNotNull(glif.outline.contours)
        assertEquals(1, glif.outline.contours!!.size)

        val contour = glif.outline.contours!![0]
        assertEquals(4, contour.points.size)

        assertEquals(0f, contour.points[0].x)
        assertEquals(0f, contour.points[0].y)
        assertEquals("line", contour.points[0].type)

        assertEquals(100f, contour.points[1].x)
        assertEquals(0f, contour.points[1].y)
    }

    @Test
    fun `parse glyph with contour identifiers and point attributes`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <glyph name="test" format="2">
              <outline>
                <contour identifier="contour1">
                  <point x="0" y="0" type="curve" smooth="yes" name="p1" identifier="point1"/>
                </contour>
              </outline>
            </glyph>
        """.trimIndent()

        val glif = GlifParser.parse(xml)

        val contour = glif.outline.contours!![0]
        assertEquals("contour1", contour.identifier)

        val point = contour.points[0]
        assertEquals(0f, point.x)
        assertEquals(0f, point.y)
        assertEquals("curve", point.type)
        assertEquals("yes", point.smooth)
        assertEquals("p1", point.name)
        assertEquals("point1", point.identifier)
    }

    @Test
    fun `parse glyph with components`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <glyph name="aacute" format="2">
              <outline>
                <component base="a"/>
                <component base="acute" xOffset="150" yOffset="200" xScale="0.8" identifier="comp1"/>
              </outline>
            </glyph>
        """.trimIndent()

        val glif = GlifParser.parse(xml)

        assertNotNull(glif.outline.components)
        assertEquals(2, glif.outline.components!!.size)

        val c1 = glif.outline.components!![0]
        assertEquals("a", c1.base)
        assertNull(c1.xOffset)

        val c2 = glif.outline.components!![1]
        assertEquals("acute", c2.base)
        assertEquals(150f, c2.xOffset)
        assertEquals(200f, c2.yOffset)
        assertEquals(0.8f, c2.xScale)
        assertEquals("comp1", c2.identifier)
    }

    @Test
    fun `parse glyph with lib`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <glyph name="test" format="2">
              <lib>
                <dict>
                  <key>public.markColor</key>
                  <string>1,0,0,1</string>
                  <key>public.verticalOrigin</key>
                  <integer>800</integer>
                </dict>
              </lib>
            </glyph>
        """.trimIndent()

        val glif = GlifParser.parse(xml)

        assertNotNull(glif.lib)
        val lib = GlyphLib(glif.lib!!.content)
        assertEquals("1,0,0,1", lib.markColor)
        assertEquals(800f, lib.verticalOrigin)
    }

    @Test
    fun `parse complex glyph from test resources`() {
        // This tests a realistic GLIF structure
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <glyph name="a" format="2">
              <advance height="750" width="388"/>
              <unicode hex="0061"/>
              <note>The letter a</note>
              <image fileName="a_sketch.png" xOffset="10" yOffset="-5"/>
              <guideline x="195" name="center"/>
              <outline>
                <contour>
                  <point x="28" y="0" type="line"/>
                  <point x="360" y="0" type="line"/>
                  <point x="360" y="486"/>
                  <point x="360" y="522" type="curve" smooth="yes"/>
                </contour>
              </outline>
            </glyph>
        """.trimIndent()

        val glif = GlifParser.parse(xml)

        assertEquals("a", glif.name)
        assertEquals(2, glif.format)
        assertEquals(750f, glif.advance.height)
        assertEquals(388f, glif.advance.width)
        assertEquals("0061", glif.unicodes!![0].hex)
        assertEquals("The letter a", glif.note)
        assertNotNull(glif.image)
        assertEquals("a_sketch.png", glif.image!!.fileName)
        assertEquals(1, glif.guidelines!!.size)
        assertEquals(195f, glif.guidelines!![0].x)
        assertEquals(1, glif.outline.contours!!.size)
        assertEquals(4, glif.outline.contours!![0].points.size)
    }

    @Test
    fun `parse glyph with minimal content`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <glyph name="space" format="2">
              <advance width="250"/>
            </glyph>
        """.trimIndent()

        val glif = GlifParser.parse(xml)

        assertEquals("space", glif.name)
        assertEquals(250f, glif.advance.width)
        assertNull(glif.advance.height)
        assertNull(glif.unicodes)
        assertNull(glif.note)
        assertNull(glif.image)
        assertNull(glif.guidelines)
        assertNull(glif.anchors)
        assertNull(glif.outline.components)
        assertNull(glif.outline.contours)
    }

    @Test
    fun `parse glyph with multiple contours`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <glyph name="i" format="2">
              <outline>
                <contour identifier="stem">
                  <point x="0" y="0" type="line"/>
                  <point x="50" y="0" type="line"/>
                  <point x="50" y="400" type="line"/>
                  <point x="0" y="400" type="line"/>
                </contour>
                <contour identifier="dot">
                  <point x="25" y="500" type="curve" smooth="yes"/>
                  <point x="50" y="500"/>
                  <point x="50" y="550"/>
                  <point x="25" y="550" type="curve" smooth="yes"/>
                </contour>
              </outline>
            </glyph>
        """.trimIndent()

        val glif = GlifParser.parse(xml)

        assertEquals(2, glif.outline.contours!!.size)
        assertEquals("stem", glif.outline.contours!![0].identifier)
        assertEquals("dot", glif.outline.contours!![1].identifier)
        assertEquals(4, glif.outline.contours!![0].points.size)
        assertEquals(4, glif.outline.contours!![1].points.size)
    }

    @Test
    fun `parse interleaved contours and components preserves order`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <glyph name="Aacute" format="2">
              <outline>
                <contour>
                  <point x="0" y="0" type="line"/>
                  <point x="100" y="0" type="line"/>
                </contour>
                <component base="A"/>
                <contour>
                  <point x="200" y="200" type="line"/>
                  <point x="300" y="300" type="line"/>
                </contour>
                <component base="acute"/>
              </outline>
            </glyph>
        """.trimIndent()

        val glif = GlifParser.parse(xml)

        // Verify the interleaved order is preserved in elements
        val elements = glif.outline.elements
        assertEquals(4, elements.size)
        assertTrue(elements[0] is Contour)
        assertTrue(elements[1] is Component)
        assertEquals("A", (elements[1] as Component).base)
        assertTrue(elements[2] is Contour)
        assertTrue(elements[3] is Component)
        assertEquals("acute", (elements[3] as Component).base)

        // Backward-compatible accessors still work
        assertEquals(2, glif.outline.components!!.size)
        assertEquals(2, glif.outline.contours!!.size)
    }
}

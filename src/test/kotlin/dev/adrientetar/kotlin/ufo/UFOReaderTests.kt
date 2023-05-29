package dev.adrientetar.kotlin.ufo

import com.google.common.truth.Truth.assertThat
import java.net.URI
import java.nio.file.Paths
import kotlin.test.Test

class UFOReaderTests {
    @Test
    fun testMetaInfo() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))

        val reader = UFOReader(ufo)
        val meta = reader.readMetaInfo()

        assertThat(meta.creator).isEqualTo("org.robofab.ufoLib")
        assertThat(meta.formatVersion).isEqualTo(3)
    }

    @Test
    fun testFontInfo() {
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))

        val reader = UFOReader(ufo)
        val info = reader.readFontInfo()

        assertThat(info.ascender).isEqualTo(850)
        assertThat(info.capHeight).isEqualTo(750)
        assertThat(info.copyright).isEqualTo("Copyright © Some Foundry.")
        assertThat(info.descender).isEqualTo(-250)
        assertThat(info.familyName).isEqualTo("Some Font (Family Name)")
        assertThat(info.italicAngle).isEqualTo(-12.5f)
        assertThat(info.note).isEqualTo("A note.")
        assertThat(info.openTypeNameDesigner).isEqualTo("Some Designer")
        assertThat(info.openTypeNameDesignerURL).isEqualTo("http://somedesigner.com")
        assertThat(info.openTypeNameLicense).isEqualTo("License info for Some Foundry.")
        assertThat(info.openTypeNameLicenseURL).isEqualTo("http://somefoundry.com/license")
        assertThat(info.openTypeNameManufacturer).isEqualTo("Some Foundry")
        assertThat(info.openTypeNameManufacturerURL).isEqualTo("http://somefoundry.com")
        assertThat(info.openTypeOS2VendorID).isEqualTo("SOME")
        //assertThat(info.postscriptBlueFuzz).isEqualTo(7)
        //assertThat(info.postscriptBlueScale).isEqualTo(7)
        //assertThat(info.postscriptBlueShift).isEqualTo(7)
        assertThat(info.postscriptBlueValues).isEqualTo(listOf(500, 510))
        //assertThat(info.postscriptDefaultCharacter).isEqualTo(".notdef")
        //assertThat(info.postscriptDefaultWidthX).isEqualTo(500)
        assertThat(info.postscriptFamilyBlues).isEqualTo(listOf(500, 510))
        assertThat(info.postscriptFamilyOtherBlues).isEqualTo(listOf(-250, -260))
        //assertThat(info.postscriptFullName).isEqualTo("SomeFont-Regular (Postscript Font Name)")
        //assertThat(info.postscriptForceBold).isEqualTo(true)
        //assertThat(info.postscriptFullName).isEqualTo("Some Font-Regular (Postscript Full Name)")
        //assertThat(info.postscriptIsFixedPitch).isEqualTo(false)
        //assertThat(info.postscriptNominalWidthX).isEqualTo(500)
        assertThat(info.postscriptOtherBlues).isEqualTo(listOf(-250, -260))
        //assertThat(info.postscriptSlantAngle).isEqualTo(-12.5f)
        assertThat(info.postscriptStemSnapH).isEqualTo(listOf(100, 120))
        assertThat(info.postscriptStemSnapV).isEqualTo(listOf(80, 90))
        //assertThat(info.postscriptUnderlinePosition).isEqualTo(-200)
        //assertThat(info.postscriptUnderlineThickness).isEqualTo(20)
        //assertThat(info.postscriptUniqueID).isEqualTo(4000000)
        //assertThat(info.postscriptWeightName).isEqualTo("medium")
        //assertThat(info.postscriptWindowsCharacterSet).isEqualTo(1)
        assertThat(info.styleMapFamilyName).isEqualTo("Some Font Regular (Style Map Family Name)")
        assertThat(info.styleMapStyleName).isEqualTo("regular")
        assertThat(info.styleName).isEqualTo("Regular (Style Name)")
        assertThat(info.trademark).isEqualTo("Trademark Some Foundry")
        assertThat(info.unitsPerEm).isEqualTo(1100)
        assertThat(info.versionMajor).isEqualTo(2)
        assertThat(info.versionMinor).isEqualTo(1)
        assertThat(info.xHeight).isEqualTo(550)
        assertThat(info.year).isEqualTo(2008)
    }

    private fun getResourceURI(name: String): URI =
        checkNotNull(
            UFOReaderTests::class.java.getResource(name)
        ).toURI()
}

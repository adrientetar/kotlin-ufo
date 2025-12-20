package io.github.adrientetar.ufo

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.io.path.writeText
import kotlin.test.Test

/**
 * Test [FontInfoValues], including reading with [UFOReader] and writing with [UFOWriter].
 */
class FontInfoTests {
    @Test
    fun testInvalidRead() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/path/to/TestFont.ufo")

        Files.createDirectories(memPath)
        val fontInfoPath = memPath.resolve("fontinfo.plist")
        val reader = UFOReader(memPath)

        // When fontinfo.plist is missing, throw UFOLibException
        // cause: NoSuchFileException
        assertThrows<UFOLibException> {
            reader.readFontInfo()
        }

        // When fontinfo.plist is empty, throw UFOLibException
        // cause: NullPointerException
        Files.createFile(fontInfoPath)
        assertThrows<UFOLibException> {
            reader.readFontInfo()
        }

        // When fontinfo.plist is invalid, throw UFOLibException
        // cause: SAXParseException
        fontInfoPath.writeText("<foo></bar>")
        assertThrows<UFOLibException> {
            reader.readFontInfo()
        }
    }

    @Test
    fun testPopulate() {
        // Populate font info and verify (test the setters)
        val info = FontInfoValues()
        populateFontInfo(info)
        verifyFontInfo(info)
    }

    @Test
    fun testRead() {
        // Read from the sample font and verify (test the reader)
        val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
        val reader = UFOReader(ufo)
        val info = reader.readFontInfo()
        verifyFontInfo(info)
    }

    @Test
    fun testWrite() {
        // Set up an in-memory filesystem
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val memPath = fs.getPath("/TestFont.ufo")

        run {
            // Read from the sample font
            val ufo = Paths.get(getResourceURI("/TestFont.ufo"))
            val reader = UFOReader(ufo)
            val info = reader.readFontInfo()

            // Write to in-memory fs
            val writer = UFOWriter(memPath)
            writer.writeFontInfo(info)
        }

        // Read from in-memory fs and verify (test the writer)
        val reader = UFOReader(memPath)
        val info = reader.readFontInfo()
        verifyFontInfo(info)
    }

    private fun populateFontInfo(info: FontInfoValues) {
        info.ascender = 850
        info.capHeight = 750
        info.copyright = "Copyright © Some Foundry."
        info.descender = -250
        info.familyName = "Some Font (Family Name)"
        info.guidelines = listOf(
            Guideline(x = 250),
            Guideline(x = -20),
            Guideline(y = 500),
            Guideline(y = -200),
            Guideline(angle = 135f, x = 0, y = 0),
            Guideline(angle = 45f, x = 0, y = 700)
        )
        info.italicAngle = -12.5f
        info.macintoshFONDFamilyID = 15000
        info.macintoshFONDName = "SomeFont Regular (FOND Name)"
        info.note = "A note."
        info.openTypeGaspRangeRecords = listOf(
            OpenTypeGaspRangeRecord(
                rangeGaspBehavior = listOf(1, 3),
                rangeMaxPPEM = 7
            ),
            OpenTypeGaspRangeRecord(
                rangeGaspBehavior = listOf(0, 1, 2, 3),
                rangeMaxPPEM = 65535
            )
        )
        info.openTypeHeadCreated = OffsetDateTime.of(
            LocalDateTime.of(2000, 1, 1, 0, 0, 0),
            ZoneOffset.UTC
        )
        info.openTypeHeadFlags = listOf(0, 1)
        info.openTypeHeadLowestRecPPEM = 10
        info.openTypeHheaAscender = 750
        info.openTypeHheaCaretOffset = 0
        info.openTypeHheaCaretSlopeRise = 1
        info.openTypeHheaCaretSlopeRun = 0
        info.openTypeHheaDescender = -250
        info.openTypeHheaLineGap = 200
        info.openTypeNameCompatibleFullName = "Some Font Regular (Compatible Full Name)"
        info.openTypeNameDescription = "Some Font by Some Designer for Some Foundry."
        info.openTypeNameDesigner = "Some Designer"
        info.openTypeNameDesignerURL = "http://somedesigner.com"
        info.openTypeNameLicense = "License info for Some Foundry."
        info.openTypeNameLicenseURL = "http://somefoundry.com/license"
        info.openTypeNameManufacturer = "Some Foundry"
        info.openTypeNameManufacturerURL = "http://somefoundry.com"
        info.openTypeNamePreferredFamilyName = "Some Font (Preferred Family Name)"
        info.openTypeNamePreferredSubfamilyName = "Regular (Preferred Subfamily Name)"
        info.openTypeNameRecords = listOf(
            OpenTypeNameRecord(
                encodingID = 0,
                languageID = 0,
                nameID = 3,
                platformID = 1,
                string = "Unique Font Identifier"
            ),
            OpenTypeNameRecord(
                encodingID = 1,
                languageID = 1033,
                nameID = 8,
                platformID = 3,
                string = "Some Foundry (Manufacturer Name)"
            )
        )
        info.openTypeNameSampleText = "Sample Text for Some Font."
        info.openTypeNameUniqueID = "OpenType name Table Unique ID"
        info.openTypeNameVersion = "OpenType name Table Version"
        info.openTypeNameWWSFamilyName = "Some Font (WWS Family Name)"
        info.openTypeNameWWSSubfamilyName = "Regular (WWS Subfamily Name)"
        info.openTypeOS2CodePageRanges = listOf(0, 1)
        info.openTypeOS2FamilyClass = listOf(1, 1)
        info.openTypeOS2Panose = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        info.openTypeOS2Selection = listOf(3)
        info.openTypeOS2StrikeoutPosition = 300
        info.openTypeOS2StrikeoutSize = 20
        info.openTypeOS2SubscriptXOffset = 0
        info.openTypeOS2SubscriptXSize = 200
        info.openTypeOS2SubscriptYOffset = -100
        info.openTypeOS2SubscriptYSize = 400
        info.openTypeOS2SuperscriptXOffset = 0
        info.openTypeOS2SuperscriptXSize = 200
        info.openTypeOS2SuperscriptYOffset = 200
        info.openTypeOS2SuperscriptYSize = 400
        info.openTypeOS2Type = listOf()
        info.openTypeOS2TypoAscender = 750
        info.openTypeOS2TypoDescender = -250
        info.openTypeOS2TypoLineGap = 200
        info.openTypeOS2UnicodeRanges = listOf(0, 1)
        info.openTypeOS2VendorID = "SOME"
        info.openTypeOS2WeightClass = 500
        info.openTypeOS2WidthClass = 5
        info.openTypeOS2WinAscent = 750
        info.openTypeOS2WinDescent = 250
        info.openTypeVheaCaretOffset = 0
        info.openTypeVheaCaretSlopeRise = 0
        info.openTypeVheaCaretSlopeRun = 1
        info.openTypeVheaVertTypoAscender = 750
        info.openTypeVheaVertTypoDescender = -250
        info.openTypeVheaVertTypoLineGap = 200
        info.postscriptBlueFuzz = 1
        info.postscriptBlueScale = 0.039625f
        info.postscriptBlueShift = 7
        info.postscriptBlueValues = listOf(500, 510)
        info.postscriptDefaultCharacter = ".notdef"
        info.postscriptDefaultWidthX = 400
        info.postscriptFamilyBlues = listOf(500, 510)
        info.postscriptFamilyOtherBlues = listOf(-250, -260)
        info.postscriptFontName = "SomeFont-Regular (Postscript Font Name)"
        info.postscriptForceBold = true
        info.postscriptFullName = "Some Font-Regular (Postscript Full Name)"
        info.postscriptIsFixedPitch = false
        info.postscriptNominalWidthX = 480
        info.postscriptOtherBlues = listOf(-250, -260)
        info.postscriptSlantAngle = -12.5f
        info.postscriptStemSnapH = listOf(100, 120)
        info.postscriptStemSnapV = listOf(80, 90)
        info.postscriptUnderlinePosition = -200
        info.postscriptUnderlineThickness = 20
        info.postscriptUniqueID = 4000000
        info.postscriptWeightName = "Medium"
        info.postscriptWindowsCharacterSet = 1
        info.styleMapFamilyName = "Some Font Regular (Style Map Family Name)"
        info.styleMapStyleName = "regular"
        info.styleName = "Regular (Style Name)"
        info.trademark = "Trademark Some Foundry"
        info.unitsPerEm = 1100
        info.versionMajor = 2
        info.versionMinor = 1
        info.xHeight = 550
        info.year = 2008
    }

    private fun verifyFontInfo(info: FontInfoValues) {
        assertThat(info.ascender).isEqualTo(850)
        assertThat(info.capHeight).isEqualTo(750)
        assertThat(info.copyright).isEqualTo("Copyright © Some Foundry.")
        assertThat(info.descender).isEqualTo(-250)
        assertThat(info.familyName).isEqualTo("Some Font (Family Name)")
        assertThat(info.guidelines).isEqualTo(
            listOf(
                Guideline(x = 250),
                Guideline(x = -20),
                Guideline(y = 500),
                Guideline(y = -200),
                Guideline(angle = 135f, x = 0, y = 0),
                Guideline(angle = 45f, x = 0, y = 700)
            )
        )
        assertThat(info.italicAngle).isEqualTo(-12.5f)
        assertThat(info.macintoshFONDFamilyID).isEqualTo(15000)
        assertThat(info.macintoshFONDName).isEqualTo("SomeFont Regular (FOND Name)")
        assertThat(info.note).isEqualTo("A note.")
        assertThat(info.openTypeGaspRangeRecords).isEqualTo(
            listOf(
                OpenTypeGaspRangeRecord(
                    rangeGaspBehavior = listOf(1, 3),
                    rangeMaxPPEM = 7
                ),
                OpenTypeGaspRangeRecord(
                    rangeGaspBehavior = listOf(0, 1, 2, 3),
                    rangeMaxPPEM = 65535
                )
            )
        )
        assertThat(info.openTypeHeadCreated).isEqualTo(
            OffsetDateTime.of(
                LocalDateTime.of(2000, 1, 1, 0, 0, 0),
                ZoneOffset.UTC
            )
        )
        assertThat(info.openTypeHeadFlags).isEqualTo(listOf(0, 1))
        assertThat(info.openTypeHeadLowestRecPPEM).isEqualTo(10)
        assertThat(info.openTypeHheaAscender).isEqualTo(750)
        assertThat(info.openTypeHheaCaretOffset).isEqualTo(0)
        assertThat(info.openTypeHheaCaretSlopeRise).isEqualTo(1)
        assertThat(info.openTypeHheaCaretSlopeRun).isEqualTo(0)
        assertThat(info.openTypeHheaDescender).isEqualTo(-250)
        assertThat(info.openTypeHheaLineGap).isEqualTo(200)
        assertThat(info.openTypeNameCompatibleFullName)
            .isEqualTo("Some Font Regular (Compatible Full Name)")
        assertThat(info.openTypeNameDescription)
            .isEqualTo("Some Font by Some Designer for Some Foundry.")
        assertThat(info.openTypeNameDesigner).isEqualTo("Some Designer")
        assertThat(info.openTypeNameDesignerURL).isEqualTo("http://somedesigner.com")
        assertThat(info.openTypeNameLicense).isEqualTo("License info for Some Foundry.")
        assertThat(info.openTypeNameLicenseURL).isEqualTo("http://somefoundry.com/license")
        assertThat(info.openTypeNameManufacturer).isEqualTo("Some Foundry")
        assertThat(info.openTypeNameManufacturerURL).isEqualTo("http://somefoundry.com")
        assertThat(info.openTypeNamePreferredFamilyName)
            .isEqualTo("Some Font (Preferred Family Name)")
        assertThat(info.openTypeNamePreferredSubfamilyName)
            .isEqualTo("Regular (Preferred Subfamily Name)")
        assertThat(info.openTypeNameRecords).isEqualTo(
            listOf(
                OpenTypeNameRecord(
                    encodingID = 0,
                    languageID = 0,
                    nameID = 3,
                    platformID = 1,
                    string = "Unique Font Identifier"
                ),
                OpenTypeNameRecord(
                    encodingID = 1,
                    languageID = 1033,
                    nameID = 8,
                    platformID = 3,
                    string = "Some Foundry (Manufacturer Name)"
                )
            )
        )
        assertThat(info.openTypeNameSampleText).isEqualTo("Sample Text for Some Font.")
        assertThat(info.openTypeNameUniqueID).isEqualTo("OpenType name Table Unique ID")
        assertThat(info.openTypeNameVersion).isEqualTo("OpenType name Table Version")
        assertThat(info.openTypeNameWWSFamilyName).isEqualTo("Some Font (WWS Family Name)")
        assertThat(info.openTypeNameWWSSubfamilyName).isEqualTo("Regular (WWS Subfamily Name)")
        assertThat(info.openTypeOS2CodePageRanges).isEqualTo(listOf(0, 1))
        assertThat(info.openTypeOS2FamilyClass).isEqualTo(listOf(1, 1))
        assertThat(info.openTypeOS2Panose).isEqualTo(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
        assertThat(info.openTypeOS2Selection).isEqualTo(listOf(3))
        assertThat(info.openTypeOS2StrikeoutPosition).isEqualTo(300)
        assertThat(info.openTypeOS2StrikeoutSize).isEqualTo(20)
        assertThat(info.openTypeOS2SubscriptXOffset).isEqualTo(0)
        assertThat(info.openTypeOS2SubscriptXSize).isEqualTo(200)
        assertThat(info.openTypeOS2SubscriptYOffset).isEqualTo(-100)
        assertThat(info.openTypeOS2SubscriptYSize).isEqualTo(400)
        assertThat(info.openTypeOS2SuperscriptXOffset).isEqualTo(0)
        assertThat(info.openTypeOS2SuperscriptXSize).isEqualTo(200)
        assertThat(info.openTypeOS2SuperscriptYOffset).isEqualTo(200)
        assertThat(info.openTypeOS2SuperscriptYSize).isEqualTo(400)
        assertThat(info.openTypeOS2Type).isEqualTo(listOf<Int>())
        assertThat(info.openTypeOS2TypoAscender).isEqualTo(750)
        assertThat(info.openTypeOS2TypoDescender).isEqualTo(-250)
        assertThat(info.openTypeOS2TypoLineGap).isEqualTo(200)
        assertThat(info.openTypeOS2UnicodeRanges).isEqualTo(listOf(0, 1))
        assertThat(info.openTypeOS2VendorID).isEqualTo("SOME")
        assertThat(info.openTypeOS2WeightClass).isEqualTo(500)
        assertThat(info.openTypeOS2WidthClass).isEqualTo(5)
        assertThat(info.openTypeOS2WinAscent).isEqualTo(750)
        assertThat(info.openTypeOS2WinDescent).isEqualTo(250)
        assertThat(info.openTypeVheaCaretOffset).isEqualTo(0)
        assertThat(info.openTypeVheaCaretSlopeRise).isEqualTo(0)
        assertThat(info.openTypeVheaCaretSlopeRun).isEqualTo(1)
        assertThat(info.openTypeVheaVertTypoAscender).isEqualTo(750)
        assertThat(info.openTypeVheaVertTypoDescender).isEqualTo(-250)
        assertThat(info.openTypeVheaVertTypoLineGap).isEqualTo(200)
        assertThat(info.postscriptBlueFuzz).isEqualTo(1)
        assertThat(info.postscriptBlueScale).isEqualTo(0.039625f)
        assertThat(info.postscriptBlueShift).isEqualTo(7)
        assertThat(info.postscriptBlueValues).isEqualTo(listOf(500, 510))
        assertThat(info.postscriptDefaultCharacter).isEqualTo(".notdef")
        assertThat(info.postscriptDefaultWidthX).isEqualTo(400)
        assertThat(info.postscriptFamilyBlues).isEqualTo(listOf(500, 510))
        assertThat(info.postscriptFamilyOtherBlues).isEqualTo(listOf(-250, -260))
        assertThat(info.postscriptFontName).isEqualTo("SomeFont-Regular (Postscript Font Name)")
        assertThat(info.postscriptForceBold).isEqualTo(true)
        assertThat(info.postscriptFullName).isEqualTo("Some Font-Regular (Postscript Full Name)")
        assertThat(info.postscriptIsFixedPitch).isEqualTo(false)
        assertThat(info.postscriptNominalWidthX).isEqualTo(480)
        assertThat(info.postscriptOtherBlues).isEqualTo(listOf(-250, -260))
        assertThat(info.postscriptSlantAngle).isEqualTo(-12.5f)
        assertThat(info.postscriptStemSnapH).isEqualTo(listOf(100, 120))
        assertThat(info.postscriptStemSnapV).isEqualTo(listOf(80, 90))
        assertThat(info.postscriptUnderlinePosition).isEqualTo(-200)
        assertThat(info.postscriptUnderlineThickness).isEqualTo(20)
        assertThat(info.postscriptUniqueID).isEqualTo(4000000)
        assertThat(info.postscriptWeightName).isEqualTo("Medium")
        assertThat(info.postscriptWindowsCharacterSet).isEqualTo(1)
        assertThat(info.styleMapFamilyName)
            .isEqualTo("Some Font Regular (Style Map Family Name)")
        assertThat(info.styleMapStyleName).isEqualTo("regular")
        assertThat(info.styleName).isEqualTo("Regular (Style Name)")
        assertThat(info.trademark).isEqualTo("Trademark Some Foundry")
        assertThat(info.unitsPerEm).isEqualTo(1100)
        assertThat(info.versionMajor).isEqualTo(2)
        assertThat(info.versionMinor).isEqualTo(1)
        assertThat(info.xHeight).isEqualTo(550)
        assertThat(info.year).isEqualTo(2008)
    }
}

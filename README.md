<div align="center">

kotlin-ufo
==========

**A library to read/write [UFO fonts]**

[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7f52ff.svg)](https://kotlinlang.org/)
[![Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE.txt)
[![Maven central](https://img.shields.io/maven-central/v/io.github.adrientetar/kotlin-ufo?color=brightgreen)](https://central.sonatype.com/artifact/io.github.adrientetar/kotlin-ufo)
[![Code coverage](https://codecov.io/gh/adrientetar/kotlin-ufo/branch/main/graph/badge.svg?token=6VLVM9MTQM)](https://codecov.io/gh/adrientetar/kotlin-ufo)

</div>

With this library, one can read and write [UFO fonts], which in turn allows using the [fontmake] compiler.

UFO 3 is supported, as well as UFOZ (ZIP-compressed UFO). UFO 2 import is supported.

Maven library
-------------

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.adrientetar:kotlin-ufo:1.1.0")
}
```

Usage
-----

### Read

```kotlin
import com.google.common.truth.Truth.assertThat
import io.github.adrientetar.ufo.UFOReader
import java.nio.file.Paths

val path = Paths.get("/usr/share/MyFont-Regular.ufo")

val reader = UFOReader(path)
val info = reader.readFontInfo()

assertThat(info.familyName).isEqualTo("My Font")
assertThat(info.styleName).isEqualTo("Regular")
```

### Write

```kotlin
import io.github.adrientetar.ufo.FontInfoValues
import io.github.adrientetar.ufo.UFOWriter
import java.nio.file.Paths

val path = Paths.get("/usr/share/MyFont-Regular.ufo")

val info = FontInfoValues().apply {
    familyName = "My Font"
    styleName = "Regular"
}

val writer = UFOWriter(path)
writer.writeMetaInfo()
writer.writeFontInfo(info)
```

### Round-trip

Read all layers from one UFO and write them to another, using the
`UFOFormatWriter` interface to target either `.ufo` or `.ufoz`:

```kotlin
import io.github.adrientetar.ufo.*
import java.nio.file.Paths

val input = Paths.get("MyFont-Regular.ufo")
val output = Paths.get("MyFont-Regular.ufoz")

val reader = UFOReader(input)

// UFOFormatWriter lets you write to .ufo or .ufoz interchangeably
val writer: UFOFormatWriter = UFOZWriter.open(output)
writer.use {
    it.writeMetaInfo()
    it.writeFontInfo(reader.readFontInfo())
    it.writeLayers(reader.readLayers())
    it.writeGroups(reader.readGroups())
    it.writeKerning(reader.readKerning())
    it.writeLib(reader.readLib())
    it.writeFeatures(reader.readFeatures())
}
```

See [the tests](/src/test/kotlin/io/github/adrientetar/ufo) for more sample code.

Supported features
------------------

| Feature | Status |
|---------|--------|
| `metainfo.plist` | ✅ Read/Write |
| `fontinfo.plist` | ✅ Read/Write (comprehensive) |
| `groups.plist` | ✅ Read/Write (with kerning group helpers) |
| `kerning.plist` | ✅ Read/Write |
| `lib.plist` | ✅ Read/Write (with `public.glyphOrder`) |
| `features.fea` | ✅ Read/Write |
| `layercontents.plist` | ✅ Read/Write (multiple layers) |
| `glyphs/` | ✅ Read/Write (all GLIF elements) |
| `images/` directory | ✅ Read/Write (PNG images) |
| `data/` directory | ✅ Read/Write (arbitrary data) |
| `UFOFormatWriter` | ✅ Shared interface for UFO/UFOZ writing |
| `readLayers()` / `readGlyphs(layerName)` | ✅ Full round-trip layer support |

**GLIF support:** advance, unicode, anchor, outline (contour, component, point), lib, note, image, guideline, and identifier attributes.

Build
-----

You need JDK 11 or later installed.

To build this library, run `./gradlew jar`.

[UFO fonts]: https://unifiedfontobject.org/
[fontmake]: https://github.com/googlefonts/fontmake
[jimfs]: https://github.com/google/jimfs

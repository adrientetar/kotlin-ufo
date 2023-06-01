<div align="center">

kotlin-ufo
==========

**A library to read/write [UFO fonts]**

[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7f52ff.svg)](https://kotlinlang.org/)
[![Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE.txt)
[![Code coverage](https://codecov.io/gh/adrientetar/kotlin-ufo/branch/main/graph/badge.svg?token=6VLVM9MTQM)](https://codecov.io/gh/adrientetar/kotlin-ufo)

</div>

With this library, one can generate [UFO fonts], which in turn allows using the [fontmake] compiler.

This project is currently in very early stage and cannot be used to make fonts. Only UFO 3 is
supported.

Usage
-----

### Read

```kotlin
import com.google.common.truth.Truth.assertThat
import dev.adrientetar.kotlin.ufo.UFOReader
import java.nio.file.Paths

val ufo = Paths.get("/usr/share/MyFont-Regular.ufo")

val reader = UFOReader(ufo)
val info = reader.readFontInfo()

assertThat(info.familyName).isEqualTo("My Font")
assertThat(info.styleName).isEqualTo("Regular")
```

See [the tests](/src/test/kotlin/dev/adrientetar/kotlin/ufo/UFOReaderTests.kt) for more sample code.

Contributions
-------------

I would like to have help with the following:

- Writing tests: we can port some tests from [fontTools.ufoLib], and write to a
  [virtual filesystem][jimfs] to keep the tests fast to run.
- Adding a pipeline to [validate exported API][binary-compatibility-validator], so that we don’t export
  unintended symbols in the library and can monitor backwards incompatible changes.
- Adding support to read/write more things (with tests 😀)

If you want to make a non-trivial contribution, consider coordinating with me and sharing design
details beforehand if applicable. Thanks!

[UFO fonts]: https://unifiedfontobject.org/
[binary-compatibility-validator]: https://github.com/Kotlin/binary-compatibility-validator
[fontTools.ufoLib]: https://github.com/fonttools/fonttools/blob/main/Tests/ufoLib/UFO3_test.py
[fontmake]: https://github.com/googlefonts/fontmake
[jimfs]: https://github.com/google/jimfs

<div align="center">

kotlin-ufo
==========

**A library to read/write [UFO fonts]**

[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7f52ff.svg)](https://kotlinlang.org/)
[![Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE.txt)
![Platform support](https://img.shields.io/badge/Platform-JVM-7e7e7e)

</div>

With this library, one can generate [UFO fonts], which in turn allows using the [fontmake] compiler.

This project is currently in very early stage and cannot be used to make fonts. Only UFO 3 is
supported.

Contributions
-------------

I would like to have help with the following:

- Writing tests: we can port some tests from [fontTools.ufoLib], and perhaps write to a
  [virtual filesystem][jimfs] to keep the tests fast to run.
- Set up continuous integration: to run tests and compute code coverage.
- Adding support to read/write more things (but it would be kinda cool to add tests first 😀)

If you want to make a non-trivial contribution, consider coordinating with me and sharing design
details beforehand if applicable. Thanks!

[UFO fonts]: https://unifiedfontobject.org/
[fontTools.ufoLib]: https://github.com/fonttools/fonttools/blob/main/Tests/ufoLib/UFO3_test.py
[fontmake]: https://github.com/googlefonts/fontmake
[jimfs]: https://github.com/google/jimfs
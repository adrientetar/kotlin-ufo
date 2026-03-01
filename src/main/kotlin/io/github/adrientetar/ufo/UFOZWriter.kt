package io.github.adrientetar.ufo

import java.io.IOException
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.name

/**
 * Writes a UFO font into a UFOZ (ZIP-compressed UFO) file.
 *
 * Usage:
 * ```kotlin
 * UFOZWriter.open(Paths.get("MyFont.ufoz")).use { writer ->
 *     writer.writeMetaInfo()
 *     writer.writeFontInfo(info)
 *     writer.writeGlyphs(glyphs)
 * }
 * ```
 *
 * Writes to a temporary directory, then packages it into a ZIP on [close].
 */
class UFOZWriter private constructor(
    private val outputPath: Path,
    private val tempDir: Path,
    private val innerWriter: UFOWriter
) : UFOFormatWriter {

    companion object {
        /**
         * Creates a UFOZ writer that will write to [path].
         *
         * @param path The output `.ufoz` file path
         * @param ufoDirectoryName The name of the `.ufo` directory inside the ZIP.
         *  Defaults to the output filename with `.ufoz` replaced by `.ufo`.
         */
        fun open(
            path: Path,
            ufoDirectoryName: String = path.name.removeSuffix(".ufoz") + ".ufo"
        ): UFOZWriter {
            val tempDir = Files.createTempDirectory("ufoz-")
            val ufoDir = tempDir.resolve(ufoDirectoryName)
            val writer = UFOWriter(ufoDir)
            return UFOZWriter(path, tempDir, writer)
        }
    }

    /** Delegates to the inner [UFOWriter]. */
    override fun writeMetaInfo() = innerWriter.writeMetaInfo()
    override fun writeFontInfo(values: FontInfoValues) = innerWriter.writeFontInfo(values)
    override fun writeLayers(layers: List<Layer>) = innerWriter.writeLayers(layers)
    override fun writeLayerGlyphs(layer: Layer) = innerWriter.writeLayerGlyphs(layer)
    override fun writeGlyphs(glyphs: List<GlyphValues>) = innerWriter.writeGlyphs(glyphs)
    override fun writeGroups(values: GroupsValues) = innerWriter.writeGroups(values)
    override fun writeKerning(values: KerningValues) = innerWriter.writeKerning(values)
    override fun writeLib(values: LibValues) = innerWriter.writeLib(values)
    override fun writeFeatures(values: FeaturesValues) = innerWriter.writeFeatures(values)
    override fun images(): ImagesDirectory = innerWriter.images()
    override fun data(): DataDirectory = innerWriter.data()

    /**
     * Packages the temporary UFO directory into a ZIP file at the output path, then
     * cleans up the temporary directory.
     */
    @OptIn(ExperimentalPathApi::class)
    override fun close() {
        try {
            // Delete existing output file
            Files.deleteIfExists(outputPath)

            // Create ZIP filesystem at output path
            val zipUri = URI.create("jar:${outputPath.toUri()}")
            val env = mapOf("create" to "true")
            FileSystems.newFileSystem(zipUri, env).use { zipFs ->
                val zipRoot = zipFs.getPath("/")
                // Walk the temp dir and copy everything into the ZIP
                Files.walkFileTree(tempDir, object : SimpleFileVisitor<Path>() {
                    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val relative = tempDir.relativize(dir).toString()
                        if (relative.isNotEmpty()) {
                            Files.createDirectories(zipRoot.resolve(relative))
                        }
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val relative = tempDir.relativize(file).toString()
                        Files.copy(file, zipRoot.resolve(relative))
                        return FileVisitResult.CONTINUE
                    }
                })
            }
        } catch (ex: IOException) {
            throw UFOLibException("Failed to write UFOZ file", ex)
        } finally {
            // Clean up temp directory
            try {
                tempDir.deleteRecursively()
            } catch (_: IOException) {
                // Best-effort cleanup
            }
        }
    }
}

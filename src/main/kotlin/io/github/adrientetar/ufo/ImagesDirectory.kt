package io.github.adrientetar.ufo

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

/**
 * Handles the images/ directory in a UFO.
 *
 * The images directory contains PNG images that can be referenced by glyph image elements.
 * All images must be in PNG format. Subdirectories are not allowed.
 */
class ImagesDirectory(private val imagesPath: Path) {

    /**
     * Returns true if the images directory exists.
     */
    val exists: Boolean
        get() = imagesPath.exists() && imagesPath.isDirectory()

    /**
     * Lists all image file names in the images directory.
     *
     * @return List of image file names (e.g., ["a_sketch.png", "logo.png"])
     */
    fun listImages(): List<String> {
        if (!exists) return emptyList()

        return Files.list(imagesPath).use { stream ->
            stream
                .filter { it.isRegularFile() }
                .map { it.name }
                .collect(Collectors.toList())
        }
    }

    /**
     * Checks if an image with the given file name exists.
     *
     * @param fileName The image file name (not a path)
     * @return true if the image exists
     */
    fun hasImage(fileName: String): Boolean {
        return imagesPath.resolve(fileName).let { it.exists() && it.isRegularFile() }
    }

    /**
     * Reads an image file as bytes.
     *
     * @param fileName The image file name (not a path)
     * @return The image data as a byte array
     * @throws UFOLibException if the image cannot be read
     */
    fun readImage(fileName: String): ByteArray {
        val imagePath = imagesPath.resolve(fileName)
        return try {
            imagePath.readBytes()
        } catch (ex: Exception) {
            throw UFOLibException("Failed to read image: $fileName", ex)
        }
    }

    /**
     * Reads an image file as bytes, or returns null if it doesn't exist.
     *
     * @param fileName The image file name (not a path)
     * @return The image data as a byte array, or null if the image doesn't exist
     */
    fun readImageOrNull(fileName: String): ByteArray? {
        val imagePath = imagesPath.resolve(fileName)
        return if (imagePath.exists() && imagePath.isRegularFile()) {
            try {
                imagePath.readBytes()
            } catch (ex: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Writes an image file.
     *
     * Creates the images directory if it doesn't exist.
     *
     * @param fileName The image file name (not a path)
     * @param data The image data as a byte array (should be PNG format)
     * @throws UFOLibException if the image cannot be written
     */
    fun writeImage(fileName: String, data: ByteArray) {
        try {
            if (!exists) {
                Files.createDirectories(imagesPath)
            }
            imagesPath.resolve(fileName).writeBytes(data)
        } catch (ex: Exception) {
            throw UFOLibException("Failed to write image: $fileName", ex)
        }
    }

    /**
     * Deletes an image file.
     *
     * @param fileName The image file name (not a path)
     * @return true if the image was deleted, false if it didn't exist
     * @throws UFOLibException if deletion fails
     */
    fun deleteImage(fileName: String): Boolean {
        val imagePath = imagesPath.resolve(fileName)
        return try {
            Files.deleteIfExists(imagePath)
        } catch (ex: Exception) {
            throw UFOLibException("Failed to delete image: $fileName", ex)
        }
    }

    /**
     * Copies all images from another ImagesDirectory.
     *
     * @param source The source images directory
     */
    fun copyFrom(source: ImagesDirectory) {
        if (!source.exists) return

        for (fileName in source.listImages()) {
            writeImage(fileName, source.readImage(fileName))
        }
    }

    companion object {
        const val DIRECTORY_NAME = "images"
    }
}

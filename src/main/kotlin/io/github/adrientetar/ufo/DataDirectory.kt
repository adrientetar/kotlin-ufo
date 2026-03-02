package io.github.adrientetar.ufo

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Collectors
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

/**
 * Handles the data/ directory in a UFO.
 *
 * The data directory allows authoring tools to store application-specific data that is
 * too complex or too large for lib.plist. Items within the directory may be either
 * plain files or directories. The top level files and directories must follow the
 * reverse domain naming scheme (e.g., "com.mycompany.mydata").
 *
 * The pattern "public.*" is reserved for standardized directory and file names.
 */
class DataDirectory(private val dataPath: Path) {

    /**
     * Returns true if the data directory exists.
     */
    val exists: Boolean
        get() = dataPath.exists() && dataPath.isDirectory()

    /**
     * Lists all top-level entries (files and directories) in the data directory.
     *
     * @return List of entry names (e.g., ["com.mycompany.settings", "org.example.cache"])
     */
    fun listEntries(): List<String> {
        if (!exists) return emptyList()

        return Files.list(dataPath).use { stream ->
            stream.map { it.name }.collect(Collectors.toList())
        }
    }

    /**
     * Checks if an entry (file or directory) with the given name exists.
     *
     * @param name The entry name (not a path)
     * @return true if the entry exists
     */
    fun hasEntry(name: String): Boolean {
        return dataPath.resolve(name).exists()
    }

    /**
     * Checks if the entry is a directory.
     *
     * @param name The entry name
     * @return true if the entry is a directory
     */
    fun isDirectory(name: String): Boolean {
        return dataPath.resolve(name).isDirectory()
    }

    /**
     * Checks if the entry is a file.
     *
     * @param name The entry name
     * @return true if the entry is a regular file
     */
    fun isFile(name: String): Boolean {
        return dataPath.resolve(name).isRegularFile()
    }

    /**
     * Reads a file from the data directory.
     *
     * @param path The relative path within the data directory (e.g., "com.mycompany/settings.json")
     * @return The file contents as a byte array
     * @throws UFOLibException if the file cannot be read
     */
    fun readFile(path: String): ByteArray {
        val filePath = dataPath.resolve(path)
        return try {
            filePath.readBytes()
        } catch (ex: Exception) {
            throw UFOLibException("Failed to read data file: $path", ex)
        }
    }

    /**
     * Reads a file from the data directory, or returns null if it doesn't exist.
     *
     * @param path The relative path within the data directory
     * @return The file contents as a byte array, or null if the file doesn't exist
     */
    fun readFileOrNull(path: String): ByteArray? {
        val filePath = dataPath.resolve(path)
        return if (filePath.exists() && filePath.isRegularFile()) {
            try {
                filePath.readBytes()
            } catch (ex: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Reads a file as a UTF-8 string.
     *
     * @param path The relative path within the data directory
     * @return The file contents as a string
     * @throws UFOLibException if the file cannot be read
     */
    fun readFileAsString(path: String): String {
        return readFile(path).toString(Charsets.UTF_8)
    }

    /**
     * Reads a file as a UTF-8 string, or returns null if it doesn't exist.
     *
     * @param path The relative path within the data directory
     * @return The file contents as a string, or null if the file doesn't exist
     */
    fun readFileAsStringOrNull(path: String): String? {
        return readFileOrNull(path)?.toString(Charsets.UTF_8)
    }

    /**
     * Writes a file to the data directory.
     *
     * Creates the data directory and any necessary parent directories if they don't exist.
     *
     * @param path The relative path within the data directory (e.g., "com.mycompany/settings.json")
     * @param data The file contents as a byte array
     * @throws UFOLibException if the file cannot be written
     */
    fun writeFile(path: String, data: ByteArray) {
        val filePath = dataPath.resolve(path)
        try {
            Files.createDirectories(filePath.parent)
            filePath.writeBytes(data)
        } catch (ex: Exception) {
            throw UFOLibException("Failed to write data file: $path", ex)
        }
    }

    /**
     * Writes a string as a UTF-8 file to the data directory.
     *
     * @param path The relative path within the data directory
     * @param content The file contents as a string
     * @throws UFOLibException if the file cannot be written
     */
    fun writeFileAsString(path: String, content: String) {
        writeFile(path, content.toByteArray(Charsets.UTF_8))
    }

    /**
     * Deletes a file or directory from the data directory.
     *
     * If the path is a directory, it and all its contents will be deleted.
     *
     * @param path The relative path within the data directory
     * @return true if something was deleted, false if nothing existed at that path
     * @throws UFOLibException if deletion fails
     */
    @OptIn(ExperimentalPathApi::class)
    fun delete(path: String): Boolean {
        val targetPath = dataPath.resolve(path)
        if (!targetPath.exists()) return false

        return try {
            if (targetPath.isDirectory()) {
                targetPath.deleteRecursively()
            } else {
                Files.delete(targetPath)
            }
            true
        } catch (ex: Exception) {
            throw UFOLibException("Failed to delete data entry: $path", ex)
        }
    }

    /**
     * Lists all files within a directory in the data directory.
     *
     * @param directoryPath The relative path to the directory
     * @return List of relative paths to all files within the directory (recursively)
     */
    fun listFilesInDirectory(directoryPath: String): List<String> {
        val dirPath = dataPath.resolve(directoryPath)
        if (!dirPath.exists() || !dirPath.isDirectory()) return emptyList()

        val files = mutableListOf<String>()
        Files.walkFileTree(dirPath, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val relativePath = dataPath.relativize(file).toString()
                files.add(relativePath)
                return FileVisitResult.CONTINUE
            }
        })
        return files
    }

    /**
     * Copies an entire entry (file or directory) from another DataDirectory.
     *
     * @param source The source data directory
     * @param entryName The name of the entry to copy
     */
    @OptIn(ExperimentalPathApi::class)
    fun copyEntryFrom(source: DataDirectory, entryName: String) {
        if (!source.hasEntry(entryName)) return

        val sourcePath = source.dataPath.resolve(entryName)
        val targetPath = dataPath.resolve(entryName)

        try {
            if (!exists) {
                Files.createDirectories(dataPath)
            }

            if (sourcePath.isDirectory()) {
                sourcePath.copyToRecursively(targetPath, followLinks = false, overwrite = true)
            } else {
                Files.createDirectories(targetPath.parent)
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (ex: Exception) {
            throw UFOLibException("Failed to copy data entry: $entryName", ex)
        }
    }

    /**
     * Copies all entries from another DataDirectory.
     *
     * @param source The source data directory
     */
    fun copyFrom(source: DataDirectory) {
        if (!source.exists) return

        for (entryName in source.listEntries()) {
            copyEntryFrom(source, entryName)
        }
    }

    companion object {
        const val DIRECTORY_NAME = "data"
    }
}

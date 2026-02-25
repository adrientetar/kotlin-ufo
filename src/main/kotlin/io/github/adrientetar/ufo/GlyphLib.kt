package io.github.adrientetar.ufo

import com.dd.plist.NSDictionary
import com.dd.plist.XMLPropertyListWriter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern

/**
 * Represents the lib element in a GLIF file.
 *
 * The lib element contains arbitrary custom data as a plist dictionary embedded in XML.
 * Common keys include:
 * - `public.markColor` - Color for marking glyphs in editors
 * - `public.verticalOrigin` - Vertical origin for vertical layout
 *
 * See: https://unifiedfontobject.org/versions/ufo3/glyphs/glif/#lib
 */
class GlyphLib(internal val dict: NSDictionary = NSDictionary()) {

    /**
     * Returns true if the lib contains the given key.
     */
    fun containsKey(key: String): Boolean =
        dict.containsKey(key)

    /**
     * Gets a value from the lib.
     */
    operator fun get(key: String): Any? =
        dict[key]?.toJavaObject()

    /**
     * Sets a value in the lib.
     */
    operator fun set(key: String, value: Any?) {
        dict.putOpt(key, value)
    }

    /**
     * Removes a key from the lib.
     */
    fun remove(key: String) {
        dict.remove(key)
    }

    /**
     * Returns all keys in the lib.
     */
    val keys: Set<String>
        get() = dict.allKeys().toSet()

    /**
     * Returns true if the lib is empty.
     */
    val isEmpty: Boolean
        get() = dict.count() == 0

    /**
     * The mark color for this glyph (e.g., "1,0,0,1" for red).
     *
     * See: https://unifiedfontobject.org/versions/ufo3/glyphs/glif/#publicmarkcolor
     */
    var markColor: String?
        get() = dict.optString(PUBLIC_MARK_COLOR)
        set(value) { dict.putOpt(PUBLIC_MARK_COLOR, value) }

    /**
     * The vertical origin Y coordinate for this glyph.
     *
     * See: https://unifiedfontobject.org/versions/ufo3/glyphs/glif/#publicverticalorigin
     */
    var verticalOrigin: Float?
        get() = dict.optFloat(PUBLIC_VERTICAL_ORIGIN)
        set(value) { dict.putOpt(PUBLIC_VERTICAL_ORIGIN, value) }

    companion object {
        const val PUBLIC_MARK_COLOR = "public.markColor"
        const val PUBLIC_VERTICAL_ORIGIN = "public.verticalOrigin"
    }
}

/**
 * Wrapper for serializing/deserializing the lib element in GLIF XML.
 *
 * The lib element in GLIF contains a plist dict directly as XML content.
 */
@Serializable(with = GlifLibSerializer::class)
data class GlifLib(
    val content: NSDictionary = NSDictionary()
)

/**
 * Custom serializer for the GlifLib element.
 */
object GlifLibSerializer : KSerializer<GlifLib> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("GlifLib", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: GlifLib) {
        if (value.content.count() == 0) {
            encoder.encodeString("")
            return
        }
        // Convert NSDictionary to plist XML string (without XML declaration)
        val baos = ByteArrayOutputStream()
        XMLPropertyListWriter.write(value.content, baos)
        val fullXml = baos.toString(Charsets.UTF_8)

        // Extract just the <dict>...</dict> part (skip XML declaration and plist wrapper)
        val dictContent = extractDictFromPlistXml(fullXml)
        encoder.encodeString(dictContent)
    }

    override fun deserialize(decoder: Decoder): GlifLib {
        val content = decoder.decodeString().trim()
        if (content.isEmpty()) {
            return GlifLib()
        }

        val dict = parseDictFromXml(content)
        return if (dict != null) GlifLib(dict) else GlifLib()
    }
}

// Pre-compiled pattern for extracting lib content - avoids recompilation on each call
private val LIB_PATTERN = Pattern.compile("<lib>\\s*(.*?)\\s*</lib>", Pattern.DOTALL)

/**
 * Extracts the lib dictionary from raw GLIF XML content.
 * This is needed because xmlutil has trouble with the nested plist format.
 */
internal fun extractLibFromGlifXml(glifXml: String): NSDictionary {
    // Find <lib>...</lib> section
    val matcher = LIB_PATTERN.matcher(glifXml)

    if (!matcher.find()) {
        return NSDictionary()
    }

    val libContent = matcher.group(1).trim()
    return parseDictFromXml(libContent) ?: NSDictionary()
}

/**
 * Serializes an NSDictionary to XML format suitable for embedding in a GLIF lib element.
 */
internal fun serializeLibToXml(dict: NSDictionary): String {
    val baos = ByteArrayOutputStream()
    XMLPropertyListWriter.write(dict, baos)
    val fullXml = baos.toString(Charsets.UTF_8)

    val dictContent = extractDictFromPlistXml(fullXml)

    // Indent the content for nicer formatting
    val indentedDict = dictContent.lines().joinToString("\n") { "  $it" }
    return "<lib>\n$indentedDict\n</lib>"
}

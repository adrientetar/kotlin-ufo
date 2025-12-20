package io.github.adrientetar.ufo

/**
 * Represents the OpenType features definitions for the font.
 *
 * The features are stored as plain text in the Adobe .fea format.
 * See [OpenType Feature File Specification](https://adobe-type-tools.github.io/afdko/OpenTypeFeatureFileSpecification.html)
 * for the format details.
 *
 * @property text The raw feature file text content. If null, no features are defined.
 */
class FeaturesValues(
    var text: String? = null
) {
    /**
     * Returns true if the features file has content.
     */
    val isEmpty: Boolean
        get() = text.isNullOrBlank()

    /**
     * Returns the text content, or an empty string if not set.
     */
    fun textOrEmpty(): String = text ?: ""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FeaturesValues) return false
        return text == other.text
    }

    override fun hashCode(): Int {
        return text?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "FeaturesValues(text=${text?.take(50)}${if ((text?.length ?: 0) > 50) "..." else ""})"
    }
}

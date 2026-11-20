package com.example.sarisari.manager.core.util

/** The single place where centavo amounts are formatted for display and parsed from user input. */
object Money {

    private const val PESO_SIGN = "\u20B1"
    private const val MAX_WHOLE_DIGITS = 10

    /** Formats [centavos] as "?1,234.50" using integer arithmetic only. */
    fun format(centavos: Long): String {
        val negative = centavos < 0
        val absolute = if (negative) -centavos else centavos
        val whole = (absolute / 100).toString().reversed().chunked(3).joinToString(",").reversed()
        val cents = (absolute % 100).toString().padStart(2, '0')
        return "${if (negative) "-" else ""}$PESO_SIGN$whole.$cents"
    }

    /**
     * Parses text typed by the owner ("12", "12.5", "1,200.75", "?5") into centavos.
     * Returns null for blank, negative, malformed, or more than two decimal places.
     */
    fun parse(input: String): Long? {
        val cleaned = input.trim().removePrefix(PESO_SIGN).replace(",", "").trim()
        val match = Regex("""^(\d{1,$MAX_WHOLE_DIGITS})(?:\.(\d{1,2}))?$""").matchEntire(cleaned) ?: return null
        val whole = match.groupValues[1].toLong()
        val cents = match.groupValues[2].padEnd(2, '0').ifEmpty { "00" }.toLong()
        return whole * 100 + cents
    }

    /** Formats centavos for an editable text field, without the peso sign or grouping ("12.50"). */
    fun toInputText(centavos: Long): String =
        "${centavos / 100}.${(centavos % 100).toString().padStart(2, '0')}"
}

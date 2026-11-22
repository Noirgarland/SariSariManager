package com.example.sarisari.manager.core.util

import kotlin.math.absoluteValue

/** The single place where centavo amounts are formatted for display and parsed from user input. */
object Money {

    private const val PESO_SIGN = "\u20B1"
    private const val MAX_WHOLE_DIGITS = 10

    private val PLAIN_WHOLE = Regex("""^\d+$""")

    /** Thousands grouping: "1,234" or "12,345,678". Each group after the first has exactly three digits. */
    private val GROUPED_WHOLE = Regex("""^[1-9]\d{0,2}(?:,\d{3})+$""")

    private val FRACTION = Regex("""^\d{1,2}$""")

    /** Formats [centavos] as "₱1,234.50" using integer arithmetic only. Safe for every Long value. */
    fun format(centavos: Long): String {
        val negative = centavos < 0
        // Divide before taking the absolute value, so Long.MIN_VALUE does not overflow.
        val whole = (centavos / 100).absoluteValue.toString().reversed().chunked(3).joinToString(",").reversed()
        val cents = (centavos % 100).absoluteValue.toString().padStart(2, '0')
        return "${if (negative) "-" else ""}$PESO_SIGN$whole.$cents"
    }

    /**
     * Parses text typed by the owner ("12", "12.5", "1,200.75", "₱5") into centavos.
     * Commas are accepted only as correct thousands separators, so "1,2" or "12,34" are rejected.
     * Returns null for blank, negative, malformed, more than two decimal places, or more than
     * [MAX_WHOLE_DIGITS] whole-peso digits.
     */
    fun parse(input: String): Long? {
        val cleaned = input.trim().removePrefix(PESO_SIGN).trim()
        val parts = cleaned.split('.')
        if (parts.size > 2) return null
        val wholeText = parts[0]
        val digits = when {
            PLAIN_WHOLE.matches(wholeText) -> wholeText
            GROUPED_WHOLE.matches(wholeText) -> wholeText.replace(",", "")
            else -> return null
        }
        if (digits.length > MAX_WHOLE_DIGITS) return null
        val fraction = parts.getOrNull(1)
        if (fraction != null && !FRACTION.matches(fraction)) return null
        val cents = fraction?.padEnd(2, '0')?.toLong() ?: 0L
        // At most 10 digits, so whole * 100 is below 10^12 and cannot overflow.
        return digits.toLong() * 100 + cents
    }

    /** Formats centavos for an editable text field, without the peso sign or grouping ("12.50"). */
    fun toInputText(centavos: Long): String =
        "${centavos / 100}.${(centavos % 100).toString().padStart(2, '0')}"
}

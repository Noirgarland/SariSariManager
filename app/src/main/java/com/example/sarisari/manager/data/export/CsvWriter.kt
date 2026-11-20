package com.example.sarisari.manager.data.export

import java.io.Writer

/**
 * Writes RFC 4180 CSV with CRLF line endings.
 * [text] cells hold values the owner typed and are protected against spreadsheet formula injection.
 * [number] cells hold values the app formats itself, such as quantities and amounts, and are written as is.
 */
class CsvWriter(private val out: Writer) {

    sealed interface Cell
    private data class TextCell(val value: String) : Cell
    private data class NumberCell(val value: String) : Cell

    fun writeByteOrderMark() = out.write("\uFEFF")

    fun row(vararg cells: Cell) {
        out.write(cells.joinToString(",") { cell ->
            when (cell) {
                is TextCell -> escape(protectFormula(cell.value))
                is NumberCell -> escape(cell.value)
            }
        })
        out.write("\r\n")
    }

    fun header(vararg names: String) = row(*names.map { text(it) }.toTypedArray())

    companion object {
        private val FORMULA_STARTS = setOf('=', '+', '-', '@', '\t', '\r')

        fun text(value: String?): Cell = TextCell(value.orEmpty())
        fun number(value: Long): Cell = NumberCell(value.toString())
        fun number(value: Int): Cell = NumberCell(value.toString())
        fun money(centavos: Long): Cell = NumberCell(formatMoney(centavos))

        /** Quotes a field when it contains a comma, quote, or line break, doubling inner quotes. */
        fun escape(value: String): String =
            if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
                "\"" + value.replace("\"", "\"\"") + "\""
            } else {
                value
            }

        /** Prefixes a single quote so spreadsheets show the text instead of running it as a formula. */
        fun protectFormula(value: String): String =
            if (value.isNotEmpty() && value.first() in FORMULA_STARTS) "'$value" else value

        /** Centavos to "1234.50" with integer arithmetic, no peso sign or grouping, so spreadsheets can sum it. */
        fun formatMoney(centavos: Long): String {
            val sign = if (centavos < 0) "-" else ""
            val absolute = if (centavos < 0) -centavos else centavos
            return "$sign${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
        }
    }
}

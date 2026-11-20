package com.example.sarisari.manager.domain.model

enum class ExpenseCategory {
    ELECTRICITY, WATER, RENT, TRANSPORTATION, SUPPLIES, SALARIES, OTHER;

    companion object {
        fun fromStorage(value: String): ExpenseCategory = entries.firstOrNull { it.name == value } ?: OTHER
    }
}

/** An operating expense. Stock purchases are recorded as stock receipts, never as expenses. */
data class Expense(
    val id: Long,
    val category: ExpenseCategory,
    val description: String?,
    val amountCentavos: Long,
    val spentAtMillis: Long,
)

/** A period when the store was open. [closedAtMillis] is null while it is still open. */
data class StoreSession(
    val id: Long,
    val openedAtMillis: Long,
    val closedAtMillis: Long?,
) {
    val isOpen: Boolean get() = closedAtMillis == null
}

class StoreAlreadyOpenException : Exception("The store is already open")

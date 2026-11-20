package com.example.sarisari.manager.domain.model

object StoreSettings {
    const val STORE_NAME_KEY = "store_name"
    const val MAX_STORE_NAME_LENGTH = 60

    /** Trims the name. Returns null when it is blank or longer than [MAX_STORE_NAME_LENGTH]. */
    fun normalizeStoreName(input: String): String? =
        input.trim().takeIf { it.isNotEmpty() && it.length <= MAX_STORE_NAME_LENGTH }
}

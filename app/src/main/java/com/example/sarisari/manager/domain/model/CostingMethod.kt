package com.example.sarisari.manager.domain.model

enum class CostingMethod(val storageValue: String) {
    WEIGHTED_AVERAGE("weighted_average"),
    LATEST_PURCHASE("latest_purchase");

    companion object {
        const val SETTING_KEY = "costing_method"
        val DEFAULT = WEIGHTED_AVERAGE

        fun fromStorage(value: String?): CostingMethod = entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}

package com.example.sarisari.manager.testing

import com.example.sarisari.manager.domain.model.AppSetting
import com.example.sarisari.manager.domain.model.InsufficientStockException
import com.example.sarisari.manager.domain.model.StockMovement
import com.example.sarisari.manager.domain.model.StockMovementType
import com.example.sarisari.manager.domain.repository.InventoryRepository
import com.example.sarisari.manager.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSettingsRepository(initial: Map<String, String> = emptyMap()) : SettingsRepository {
    private val values = MutableStateFlow(initial)

    private fun toSetting(key: String, value: String?) = value?.let { AppSetting(key, it, 0, 0) }

    override suspend fun getSetting(key: String): AppSetting? = toSetting(key, values.value[key])
    override fun observeSetting(key: String): Flow<AppSetting?> = values.map { toSetting(key, it[key]) }
    override suspend fun putSetting(key: String, value: String) {
        values.value = values.value + (key to value)
    }
}

/** Keeps stock and cost per product in memory and applies the same rules as the real repository. */
class FakeInventoryRepository(
    startingStock: Map<Long, Int> = emptyMap(),
    startingCost: Map<Long, Long> = emptyMap(),
) : InventoryRepository {
    val stock = startingStock.toMutableMap()
    val cost = startingCost.toMutableMap()
    val movements = mutableListOf<StockMovement>()

    override fun observeMovements(productId: Long?, limit: Int): Flow<List<StockMovement>> =
        MutableStateFlow(movements.filter { productId == null || it.productId == productId }.reversed())

    override suspend fun receiveStock(
        productId: Long,
        baseQuantity: Int,
        totalCostCentavos: Long,
        reason: String?,
        newUnitCost: (Int, Long) -> Long,
    ): Long {
        val current = stock[productId] ?: 0
        cost[productId] = newUnitCost(current, cost[productId] ?: 0)
        stock[productId] = current + baseQuantity
        return record(productId, StockMovementType.RECEIPT, baseQuantity, totalCostCentavos, reason)
    }

    override suspend fun adjustStock(
        productId: Long,
        type: StockMovementType,
        quantityChange: Int,
        reason: String?,
    ): Long {
        val current = stock[productId] ?: 0
        if (current + quantityChange < 0) throw InsufficientStockException(current, -quantityChange)
        stock[productId] = current + quantityChange
        return record(productId, type, quantityChange, null, reason)
    }

    private fun record(productId: Long, type: StockMovementType, change: Int, total: Long?, reason: String?): Long {
        val id = movements.size + 1L
        movements += StockMovement(id, productId, "P$productId", "piece", type, change, null, total, reason, id)
        return id
    }
}

/** Records completed sales in memory. Set [failWith] to make checkout throw. */
class FakeSalesRepository : com.example.sarisari.manager.domain.repository.SalesRepository {
    val completed = mutableListOf<Pair<List<com.example.sarisari.manager.domain.model.SaleLineInput>, Long>>()
    var failWith: Exception? = null

    override suspend fun completeSale(
        lines: List<com.example.sarisari.manager.domain.model.SaleLineInput>,
        amountReceivedCentavos: Long,
    ): com.example.sarisari.manager.domain.model.CompletedSale {
        failWith?.let { throw it }
        completed += lines to amountReceivedCentavos
        val total = lines.sumOf { it.lineTotalCentavos }
        return com.example.sarisari.manager.domain.model.CompletedSale(
            completed.size.toLong(), total, amountReceivedCentavos, amountReceivedCentavos - total, 1,
        )
    }

    override fun observeSales(limit: Int): Flow<List<com.example.sarisari.manager.domain.model.Sale>> =
        MutableStateFlow(emptyList())

    override fun observeSale(id: Long): Flow<com.example.sarisari.manager.domain.model.Sale?> = MutableStateFlow(null)

    override suspend fun voidSale(id: Long, reason: String?) = Unit
}

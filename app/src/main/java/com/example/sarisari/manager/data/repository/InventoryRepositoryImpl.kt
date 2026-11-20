package com.example.sarisari.manager.data.repository

import com.example.sarisari.manager.data.local.dao.StockMovementDao
import com.example.sarisari.manager.data.local.entity.StockMovementWithProduct
import com.example.sarisari.manager.domain.model.StockMovement
import com.example.sarisari.manager.domain.model.StockMovementType
import com.example.sarisari.manager.domain.repository.InventoryRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InventoryRepositoryImpl @Inject constructor(
    private val dao: StockMovementDao,
    private val clock: Clock,
) : InventoryRepository {

    override fun observeMovements(productId: Long?, limit: Int): Flow<List<StockMovement>> =
        dao.observe(productId, limit).map { list -> list.map { it.toDomain() } }

    override suspend fun receiveStock(
        productId: Long,
        baseQuantity: Int,
        totalCostCentavos: Long,
        reason: String?,
        newUnitCost: (Int, Long) -> Long,
    ): Long = dao.receive(productId, baseQuantity, totalCostCentavos, reason, clock.millis(), newUnitCost)

    override suspend fun adjustStock(
        productId: Long,
        type: StockMovementType,
        quantityChange: Int,
        reason: String?,
    ): Long = dao.adjust(productId, type.name, quantityChange, reason, clock.millis())

    private fun StockMovementWithProduct.toDomain() = StockMovement(
        id = movement.id,
        productId = movement.productId,
        productName = productName,
        baseUnitName = baseUnitName,
        type = StockMovementType.valueOf(movement.type),
        quantityChange = movement.quantityChange,
        unitCostCentavos = movement.unitCostCentavos,
        totalCostCentavos = movement.totalCostCentavos,
        reason = movement.reason,
        createdAtMillis = movement.createdAt,
    )
}

package com.example.sarisari.manager.data.repository

import com.example.sarisari.manager.data.local.dao.SaleDao
import com.example.sarisari.manager.data.local.entity.SaleItemEntity
import com.example.sarisari.manager.data.local.entity.SaleWithItems
import com.example.sarisari.manager.domain.model.CompletedSale
import com.example.sarisari.manager.domain.model.Sale
import com.example.sarisari.manager.domain.model.SaleItem
import com.example.sarisari.manager.domain.model.SaleLineInput
import com.example.sarisari.manager.domain.repository.SalesRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SalesRepositoryImpl @Inject constructor(
    private val dao: SaleDao,
    private val clock: Clock,
) : SalesRepository {

    override suspend fun completeSale(lines: List<SaleLineInput>, amountReceivedCentavos: Long): CompletedSale =
        dao.complete(lines, amountReceivedCentavos, clock.millis())

    override fun observeSales(limit: Int): Flow<List<Sale>> =
        dao.observeAll(limit).map { list -> list.map { it.toDomain() } }

    override fun observeSale(id: Long): Flow<Sale?> = dao.observeById(id).map { it?.toDomain() }

    override suspend fun voidSale(id: Long, reason: String?) = dao.voidSale(id, reason, clock.millis())

    private fun SaleWithItems.toDomain() = Sale(
        id = sale.id,
        totalCentavos = sale.totalCentavos,
        amountReceivedCentavos = sale.amountReceivedCentavos,
        changeCentavos = sale.changeCentavos,
        totalCostCentavos = sale.totalCostCentavos,
        createdAtMillis = sale.createdAt,
        voidedAtMillis = sale.voidedAt,
        voidReason = sale.voidReason,
        items = items.sortedBy { it.id }.map { it.toDomain() },
    )

    private fun SaleItemEntity.toDomain() = SaleItem(
        id = id,
        productId = productId,
        productName = productName,
        unitName = unitName,
        unitFactor = unitFactor,
        quantity = quantity,
        baseQuantity = baseQuantity,
        unitPriceCentavos = unitPriceCentavos,
        lineTotalCentavos = lineTotalCentavos,
        unitCostCentavos = unitCostCentavos,
        lineCostCentavos = lineCostCentavos,
    )
}

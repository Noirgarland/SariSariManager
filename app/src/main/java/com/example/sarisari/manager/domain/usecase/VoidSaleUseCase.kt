package com.example.sarisari.manager.domain.usecase

import com.example.sarisari.manager.domain.model.SaleAlreadyVoidedException
import com.example.sarisari.manager.domain.model.SaleNotFoundException
import com.example.sarisari.manager.domain.repository.SalesRepository
import javax.inject.Inject

enum class VoidResult { VOIDED, ALREADY_VOIDED, NOT_FOUND }

/** Voids a completed sale. The sale record is kept. Its stock is returned through compensating movements. */
class VoidSaleUseCase @Inject constructor(
    private val sales: SalesRepository,
) {
    suspend operator fun invoke(saleId: Long, reason: String?): VoidResult = try {
        sales.voidSale(saleId, reason?.trim()?.ifEmpty { null })
        VoidResult.VOIDED
    } catch (_: SaleAlreadyVoidedException) {
        VoidResult.ALREADY_VOIDED
    } catch (_: SaleNotFoundException) {
        VoidResult.NOT_FOUND
    }
}

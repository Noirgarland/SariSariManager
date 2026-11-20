package com.example.sarisari.manager.feature.cashier.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.sarisari.manager.core.navigation.SaleDetailRoute
import com.example.sarisari.manager.domain.repository.SalesRepository
import com.example.sarisari.manager.domain.usecase.VoidResult
import com.example.sarisari.manager.domain.usecase.VoidSaleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SaleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    salesRepository: SalesRepository,
    private val voidSale: VoidSaleUseCase,
) : ViewModel() {

    private val saleId: Long = savedStateHandle.toRoute<SaleDetailRoute>().saleId

    private data class Local(val isVoiding: Boolean = false, val voidFailed: Boolean = false)

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<SaleDetailUiState> = combine(salesRepository.observeSale(saleId), local) { sale, l ->
        SaleDetailUiState(isLoading = false, sale = sale, isVoiding = l.isVoiding, voidFailed = l.voidFailed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SaleDetailUiState())

    fun onVoid(reason: String) {
        if (local.value.isVoiding) return
        local.update { Local(isVoiding = true) }
        viewModelScope.launch {
            val failed = try {
                voidSale(saleId, reason) == VoidResult.NOT_FOUND
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                true
            }
            local.update { Local(voidFailed = failed) }
        }
    }

    fun onVoidErrorShown() = local.update { it.copy(voidFailed = false) }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

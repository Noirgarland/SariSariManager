package com.example.sarisari.manager.feature.settings

import com.example.sarisari.manager.domain.model.BackupError
import com.example.sarisari.manager.domain.model.BackupInfo
import com.example.sarisari.manager.domain.model.CostingMethod
import com.example.sarisari.manager.domain.usecase.ReportPeriod

enum class SettingsTask { BACKUP, CHECK_RESTORE, EXPORT }

sealed interface SettingsMessage {
    data object StoreNameSaved : SettingsMessage
    data object StoreNameInvalid : SettingsMessage
    data object CostingChanged : SettingsMessage
    data object BackupSaved : SettingsMessage
    data object BackupFailed : SettingsMessage
    data class RestoreRejected(val error: BackupError) : SettingsMessage
    data object RestoreApplied : SettingsMessage
    data object RestoreRolledBack : SettingsMessage
    data class ExportDone(val rows: Int) : SettingsMessage
    data object ExportFailed : SettingsMessage
}

data class SettingsUiState(
    val isLoading: Boolean = true,
    val storeNameInput: String = "",
    val costingMethod: CostingMethod = CostingMethod.DEFAULT,
    val task: SettingsTask? = null,
    val message: SettingsMessage? = null,
    /** A validated backup waiting for the owner to confirm the replacement. */
    val pendingRestore: BackupInfo? = null,
    val restoreStaged: Boolean = false,
    val exportMonth: ReportPeriod.Month? = null,
    val canGoNextMonth: Boolean = false,
) {
    val isBusy: Boolean get() = task != null
}

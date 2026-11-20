package com.example.sarisari.manager.di

import com.example.sarisari.manager.data.backup.BackupRepositoryImpl
import com.example.sarisari.manager.data.export.ExportRepositoryImpl
import com.example.sarisari.manager.data.repository.CategoryRepositoryImpl
import com.example.sarisari.manager.data.repository.ExpenseRepositoryImpl
import com.example.sarisari.manager.data.repository.ReportRepositoryImpl
import com.example.sarisari.manager.data.repository.StoreSessionRepositoryImpl
import com.example.sarisari.manager.data.repository.InventoryRepositoryImpl
import com.example.sarisari.manager.data.repository.ProductRepositoryImpl
import com.example.sarisari.manager.data.repository.SalesRepositoryImpl
import com.example.sarisari.manager.data.repository.SettingsRepositoryImpl
import com.example.sarisari.manager.domain.repository.BackupRepository
import com.example.sarisari.manager.domain.repository.CategoryRepository
import com.example.sarisari.manager.domain.repository.ExportRepository
import com.example.sarisari.manager.domain.repository.ExpenseRepository
import com.example.sarisari.manager.domain.repository.ReportRepository
import com.example.sarisari.manager.domain.repository.StoreSessionRepository
import com.example.sarisari.manager.domain.repository.InventoryRepository
import com.example.sarisari.manager.domain.repository.ProductRepository
import com.example.sarisari.manager.domain.repository.SalesRepository
import com.example.sarisari.manager.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    abstract fun bindInventoryRepository(impl: InventoryRepositoryImpl): InventoryRepository

    @Binds
    abstract fun bindSalesRepository(impl: SalesRepositoryImpl): SalesRepository

    @Binds
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    @Binds
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds
    abstract fun bindStoreSessionRepository(impl: StoreSessionRepositoryImpl): StoreSessionRepository

    @Binds
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    abstract fun bindExportRepository(impl: ExportRepositoryImpl): ExportRepository
}

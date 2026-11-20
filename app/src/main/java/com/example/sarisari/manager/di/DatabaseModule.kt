package com.example.sarisari.manager.di

import android.content.Context
import androidx.room.Room
import com.example.sarisari.manager.data.local.dao.AppSettingDao
import com.example.sarisari.manager.data.local.database.ALL_MIGRATIONS
import com.example.sarisari.manager.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

private const val DATABASE_NAME = "sarisari.db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        @IoDispatcher io: CoroutineDispatcher,
    ): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(*ALL_MIGRATIONS)
            .setQueryCoroutineContext(io)
            .build()

    @Provides
    fun provideAppSettingDao(db: AppDatabase): AppSettingDao = db.appSettingDao()
}

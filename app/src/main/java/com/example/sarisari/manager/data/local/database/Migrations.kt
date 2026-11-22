package com.example.sarisari.manager.data.local.database

import androidx.room.migration.Migration

/**
 * Every schema migration, sorted by start version.
 * First start is 1, each end equals the next start, last end equals [AppDatabase.VERSION].
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)

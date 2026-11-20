package com.example.sarisari.manager.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds operating expenses and store open/close sessions. */
val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`category` TEXT NOT NULL, `description` TEXT, `amount_centavos` INTEGER NOT NULL, " +
                "`spent_at` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_spent_at` ON `expenses` (`spent_at`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `store_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`opened_at` INTEGER NOT NULL, `closed_at` INTEGER, `created_at` INTEGER NOT NULL, " +
                "`updated_at` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_store_sessions_opened_at` ON `store_sessions` (`opened_at`)")
    }
}

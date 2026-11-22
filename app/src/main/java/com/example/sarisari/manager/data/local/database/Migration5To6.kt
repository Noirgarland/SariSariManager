package com.example.sarisari.manager.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds `sale_items.base_unit_name`, a snapshot of the product's base unit at sale time,
 * so reports keep the unit a quantity was counted in even after the product is edited.
 *
 * Existing rows are filled with the best information available:
 * - A line sold with factor 1 was sold in the base unit, because extra selling units need a factor of
 *   at least 2. Its `unit_name` is therefore the base unit name at sale time.
 * - Other lines take the product's current base unit name. This is the only record left of it.
 *
 * No rows are removed or rewritten except to fill the new column. Running it on a table that already
 * has the column only refills empty values, so a repeated run is harmless.
 */
val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        if (!db.hasColumn("sale_items", "base_unit_name")) {
            db.execSQL("ALTER TABLE `sale_items` ADD COLUMN `base_unit_name` TEXT NOT NULL DEFAULT ''")
        }
        db.execSQL(
            """
            UPDATE sale_items SET base_unit_name = CASE
                WHEN unit_factor = 1 THEN unit_name
                ELSE COALESCE(
                    (SELECT p.base_unit_name FROM products p WHERE p.id = sale_items.product_id),
                    unit_name
                )
            END
            WHERE base_unit_name = ''
            """.trimIndent()
        )
    }
}

private fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean =
    query("PRAGMA table_info(`$table`)").use { cursor ->
        val nameIndex = cursor.getColumnIndexOrThrow("name")
        generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }.any { it == column }
    }

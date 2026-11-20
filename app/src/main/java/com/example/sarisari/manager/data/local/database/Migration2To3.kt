package com.example.sarisari.manager.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds the stock movement history. */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `stock_movements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`product_id` INTEGER NOT NULL, `type` TEXT NOT NULL, `quantity_change` INTEGER NOT NULL, " +
                "`unit_cost_centavos` INTEGER, `total_cost_centavos` INTEGER, `reason` TEXT, " +
                "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, " +
                "FOREIGN KEY(`product_id`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_product_id` ON `stock_movements` (`product_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_created_at` ON `stock_movements` (`created_at`)")
    }
}

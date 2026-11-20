package com.example.sarisari.manager.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds sales and sale items. */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sales` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`total_centavos` INTEGER NOT NULL, `amount_received_centavos` INTEGER NOT NULL, " +
                "`change_centavos` INTEGER NOT NULL, `total_cost_centavos` INTEGER NOT NULL, " +
                "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `voided_at` INTEGER, `void_reason` TEXT)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_created_at` ON `sales` (`created_at`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sale_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`sale_id` INTEGER NOT NULL, `product_id` INTEGER NOT NULL, `product_name` TEXT NOT NULL, " +
                "`unit_name` TEXT NOT NULL, `unit_factor` INTEGER NOT NULL, `quantity` INTEGER NOT NULL, " +
                "`base_quantity` INTEGER NOT NULL, `unit_price_centavos` INTEGER NOT NULL, " +
                "`line_total_centavos` INTEGER NOT NULL, `unit_cost_centavos` INTEGER NOT NULL, " +
                "`line_cost_centavos` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, " +
                "FOREIGN KEY(`sale_id`) REFERENCES `sales`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`product_id`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_sale_id` ON `sale_items` (`sale_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_product_id` ON `sale_items` (`product_id`)")
    }
}

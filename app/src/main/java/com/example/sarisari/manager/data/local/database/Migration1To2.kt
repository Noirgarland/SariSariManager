package com.example.sarisari.manager.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds categories, products, and product units. */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL COLLATE NOCASE, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `category_id` INTEGER, `base_unit_name` TEXT NOT NULL, " +
                "`cost_centavos` INTEGER NOT NULL, `selling_price_centavos` INTEGER NOT NULL, " +
                "`stock_quantity` INTEGER NOT NULL, `low_stock_threshold` INTEGER NOT NULL, " +
                "`is_archived` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, " +
                "FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_category_id` ON `products` (`category_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_name` ON `products` (`name`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `product_units` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`product_id` INTEGER NOT NULL, `name` TEXT NOT NULL, `base_quantity` INTEGER NOT NULL, " +
                "`selling_price_centavos` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, " +
                "`updated_at` INTEGER NOT NULL, " +
                "FOREIGN KEY(`product_id`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_units_product_id` ON `product_units` (`product_id`)")
    }
}

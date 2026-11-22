package com.example.sarisari.manager.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds utang (customer credit): the `customers` and `credit_entries` tables, and on `sales` the
 * `customer_id` of an utang sale and its `credit_centavos`. Existing sales were all cash, so they get
 * no customer and 0 credit. No existing rows are changed.
 */
val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `customers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL COLLATE NOCASE, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_customers_name` ON `customers` (`name`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `credit_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`customer_id` INTEGER NOT NULL, `type` TEXT NOT NULL, `amount_centavos` INTEGER NOT NULL, " +
                "`sale_id` INTEGER, `note` TEXT, `occurred_at` INTEGER NOT NULL, `voided_at` INTEGER, " +
                "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, " +
                "FOREIGN KEY(`customer_id`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT, " +
                "FOREIGN KEY(`sale_id`) REFERENCES `sales`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_credit_entries_customer_id` ON `credit_entries` (`customer_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_credit_entries_sale_id` ON `credit_entries` (`sale_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_credit_entries_occurred_at` ON `credit_entries` (`occurred_at`)")

        // SQLite allows a REFERENCES clause in ADD COLUMN when the default is NULL, which it is here.
        db.execSQL(
            "ALTER TABLE `sales` ADD COLUMN `customer_id` INTEGER DEFAULT NULL " +
                "REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT"
        )
        db.execSQL("ALTER TABLE `sales` ADD COLUMN `credit_centavos` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_customer_id` ON `sales` (`customer_id`)")
    }
}

package com.example.sarisari.manager.testing

import android.database.sqlite.SQLiteDatabase
import java.io.File
import org.json.JSONObject

/**
 * Builds real databases at an older schema version from the committed Room schema export
 * (app/schemas/.../<version>.json), so migration tests start from exactly what shipped.
 */
object SchemaFixtures {

    private const val SCHEMA_DIR = "schemas/com.example.sarisari.manager.data.local.database.AppDatabase"

    private fun schemaFile(version: Int): File =
        listOf(File(SCHEMA_DIR), File("app/$SCHEMA_DIR")).map { File(it, "$version.json") }.first { it.exists() }

    /** Creates [file] with every table, index, and Room identity row of schema [version]. */
    fun createDatabase(file: File, version: Int, fill: (SQLiteDatabase) -> Unit = {}) {
        file.delete()
        val database = JSONObject(schemaFile(version).readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val entities = database.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = database.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            fill(db)
            db.version = version
        }
    }

    /**
     * Version 5 data with sales whose products were edited afterwards:
     * - Product 1 "Coke", base unit "bottle", also sold by the "case" of 12.
     * - Product 2 was sold as "Chips (old name)" in base unit "pack", then renamed to "Chips" with base unit "piece".
     * - Sale 1 has three items: 2 bottles, 1 case, and 1 pack of chips. Sale 2 is voided.
     */
    fun fillVersion5SalesData(db: SQLiteDatabase) {
        db.execSQL("INSERT INTO products VALUES (1, 'Coke', NULL, 'bottle', 4500, 6500, 6, 5, 0, 1, 1)")
        db.execSQL("INSERT INTO products VALUES (2, 'Chips', NULL, 'piece', 1200, 2000, 4, 3, 0, 1, 9)")
        db.execSQL("INSERT INTO product_units VALUES (1, 1, 'case', 12, 70000, 1, 1)")
        db.execSQL("INSERT INTO stock_movements VALUES (1, 1, 'RECEIPT', 20, 4500, 90000, NULL, 1, 1)")
        db.execSQL("INSERT INTO stock_movements VALUES (2, 2, 'RECEIPT', 5, 1200, 6000, NULL, 1, 1)")
        db.execSQL("INSERT INTO stock_movements VALUES (3, 1, 'SALE', -2, NULL, NULL, 'Sale #1', 2, 2)")
        db.execSQL("INSERT INTO stock_movements VALUES (4, 1, 'SALE', -12, NULL, NULL, 'Sale #1', 2, 2)")
        db.execSQL("INSERT INTO stock_movements VALUES (5, 2, 'SALE', -1, NULL, NULL, 'Sale #1', 2, 2)")
        db.execSQL("INSERT INTO stock_movements VALUES (6, 1, 'SALE', -1, NULL, NULL, 'Sale #2', 3, 3)")
        db.execSQL("INSERT INTO stock_movements VALUES (7, 1, 'SALE_VOID', 1, NULL, NULL, 'Void of sale #2', 4, 4)")
        db.execSQL("INSERT INTO sales VALUES (1, 85000, 100000, 15000, 64200, 2, 2, NULL, NULL)")
        db.execSQL("INSERT INTO sales VALUES (2, 6500, 6500, 0, 4500, 3, 4, 4, 'mistake')")
        db.execSQL("INSERT INTO sale_items VALUES (1, 1, 1, 'Coke', 'bottle', 1, 2, 2, 6500, 13000, 4500, 9000, 2, 2)")
        db.execSQL("INSERT INTO sale_items VALUES (2, 1, 1, 'Coke', 'case', 12, 1, 12, 70000, 70000, 4500, 54000, 2, 2)")
        db.execSQL("INSERT INTO sale_items VALUES (3, 1, 2, 'Chips (old name)', 'pack', 1, 1, 1, 2000, 2000, 1200, 1200, 2, 2)")
        db.execSQL("INSERT INTO sale_items VALUES (4, 2, 1, 'Coke', 'bottle', 1, 1, 1, 6500, 6500, 4500, 4500, 3, 3)")
        db.execSQL("INSERT INTO app_settings VALUES ('store_name', 'Aling Nena Store', 1, 1)")
    }
}

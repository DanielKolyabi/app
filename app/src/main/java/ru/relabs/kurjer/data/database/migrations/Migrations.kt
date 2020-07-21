package ru.relabs.kurjer.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    fun getMigrations(): Array<Migration> = arrayOf(
        migration_26_27,
        migration_27_28,
        migration_28_29,
        migration_29_30,
        migration_30_31,
        migration_31_32,
        migration_32_33,
        migration_33_34
    )


    private val migration_26_27 = object : Migration(26, 27) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE task_items ADD COLUMN need_photo INTEGER NOT NULL DEFAULT 0")
        }
    }
    private val migration_27_28 = object : Migration(27, 28) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE report_query ADD COLUMN battery_level INTEGER NOT NULL DEFAULT 0")
        }
    }
    private val migration_28_29 = object : Migration(28, 29) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                    CREATE TABLE entrances_data(
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        task_item_id INTEGER NOT NULL,
                        number INTEGER NOT NULL,
                        apartments_count INTEGER NOT NULL,
                        is_euro_boxes INTEGER NOT NULL,
                        has_lookout INTEGER NOT NULL,
                        is_stacked INTEGER NOT NULL,
                        is_refused INTEGER NOT NULL,
                        FOREIGN KEY(task_item_id) REFERENCES task_items(id) ON DELETE CASCADE
                    )
                """.trimIndent()
            )
        }
    }
    private val migration_29_30 = object : Migration(29, 30) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE tasks ADD COLUMN couple_type INTEGER NOT NULL DEFAULT 1")
        }
    }
    private val migration_30_31 = object : Migration(30, 31) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE report_query ADD COLUMN remove_after_send INTEGER NOT NULL DEFAULT 0")
        }
    }
    private val migration_31_32 = object : Migration(31, 32) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE report_query ADD COLUMN close_distance INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE report_query ADD COLUMN allowed_distance INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE report_query ADD COLUMN radius_required INTEGER NOT NULL DEFAULT 0")
        }
    }
    private val migration_32_33 = object : Migration(32, 33) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE task_item_photos ADD COLUMN entrance_number INTEGER NOT NULL DEFAULT -1")
        }
    }
    private val migration_33_34 = object : Migration(33, 34) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE entrances_data ADD COLUMN photo_required INTEGER NOT NULL DEFAULT 0")
        }
    }
}
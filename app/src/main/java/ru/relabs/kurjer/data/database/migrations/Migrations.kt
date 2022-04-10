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
        migration_33_34,
        migration_34_35,
        migration_35_36,
        migration_37_38,
        migration_38_39,
        migration_39_40,
        migration_40_41,
        migration_41_42,
        migration_42_43,
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
    private val migration_34_35 = object : Migration(34, 35) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                    CREATE TABLE report_query_temp(
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        task_item_id INTEGER NOT NULL,
                        task_id INTEGER NOT NULL,
                        image_folder_id INTEGER NOT NULL,
                        gps TEXT NOT NULL,
                        close_time INTEGER NOT NULL,
                        user_description TEXT NOT NULL,
                        entrances TEXT NOT NULL,
                        token TEXT NOT NULL,
                        battery_level INTEGER NOT NULL,
                        remove_after_send INTEGER NOT NULL,
                        close_distance INTEGER NOT NULL,
                        allowed_distance INTEGER NOT NULL,
                        radius_required INTEGER NOT NULL
                    )
                """.trimIndent()
            )
            database.execSQL("INSERT INTO report_query_temp SELECT * FROM report_query")
            database.execSQL("DROP TABLE report_query")
            database.execSQL(
                """
                    CREATE TABLE report_query(
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        task_item_id INTEGER NOT NULL,
                        task_id INTEGER NOT NULL,
                        image_folder_id INTEGER NOT NULL,
                        gps TEXT NOT NULL,
                        close_time INTEGER NOT NULL,
                        user_description TEXT NOT NULL,
                        entrances TEXT NOT NULL,
                        token TEXT NOT NULL,
                        battery_level INTEGER NOT NULL,
                        remove_after_send INTEGER NOT NULL,
                        close_distance INTEGER NOT NULL,
                        allowed_distance INTEGER NOT NULL,
                        radius_required INTEGER NOT NULL
                    )
                """.trimIndent()
            )
            database.execSQL("INSERT INTO report_query SELECT * FROM report_query_temp")
            database.execSQL("DROP TABLE report_query_temp")
        }
    }
    private val migration_35_36 = object : Migration(35, 36) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("DELETE FROM task_items")
            database.execSQL("DELETE FROM addresses")
            database.execSQL("DELETE FROM tasks")

            database.execSQL("ALTER TABLE tasks ADD COLUMN by_other_user INTEGER NOT NULL DEFAULT 0")
        }
    }
    private val migration_37_38 = object : Migration(37, 38) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE task_items ADD COLUMN is_firm INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE task_items ADD COLUMN office_name TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE task_items ADD COLUMN firm_name TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE tasks ADD COLUMN delivery_type INTEGER NOT NULL DEFAULT 1")
        }
    }
    private val migration_38_39 = object : Migration(38, 39) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE report_query ADD COLUMN is_rejected INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE report_query ADD COLUMN reject_reason TEXT NOT NULL DEFAULT ''")
        }
    }
    private val migration_39_40 = object : Migration(39, 40) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                    CREATE TABLE firm_reject_reason(
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        reason TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent()
            )
        }
    }
    private val migration_40_41 = object : Migration(40, 41) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE report_query ADD COLUMN delivery_type INTEGER NOT NULL DEFAULT 1")
        }
    }
    private val migration_41_42 = object : Migration(41, 42) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE tasks ADD COLUMN listSort TEXT NOT NULL DEFAULT ''")
        }
    }
    private val migration_42_43 = object : Migration(42, 43) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE tasks ADD COLUMN storage_lat REAL DEFAULT ''")
            database.execSQL("ALTER TABLE tasks ADD COLUMN storage_long REAL DEFAULT ''")
        }
    }
}
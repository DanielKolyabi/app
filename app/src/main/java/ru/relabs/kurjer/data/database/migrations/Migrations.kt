package ru.relabs.kurjer.data.database.migrations

import android.content.SharedPreferences
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.relabs.kurjer.domain.repositories.SettingsRepository

object Migrations : KoinComponent {
    private val preferences by inject<SharedPreferences>()

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
        migration_43_44,
        migration_44_45,
        migration_45_46,
        migration_46_47,
        migration_47_48,
        migration_48_53
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
    private val migration_43_44 = object : Migration(43, 44) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE tasks ADD COLUMN districtType INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE tasks ADD COLUMN orderNumber INTEGER NOT NULL DEFAULT -1")
        }
    }
    private val migration_44_45 = object : Migration(44, 45) {
        override fun migrate(database: SupportSQLiteDatabase) {
            val storedRequiredRadius = preferences.getInt(SettingsRepository.RADIUS_KEY, SettingsRepository.DEFAULT_REQUIRED_RADIUS)
            database.execSQL("ALTER TABLE task_items ADD COLUMN close_radius INTEGER NOT NULL DEFAULT $storedRequiredRadius")
        }
    }
    private val migration_45_46 = object : Migration(45, 46) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE task_item_result_entrances ADD COLUMN user_description TEXT NOT NULL DEFAULT ''")
        }
    }
    private val migration_46_47 = object : Migration(46, 47) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE report_query ADD COLUMN is_photo_required INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE tasks ADD COLUMN edition_photo_url TEXT DEFAULT NULL")
            database.execSQL("ALTER TABLE task_item_results ADD COLUMN is_photo_required INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE task_item_result_entrances ADD COLUMN is_photo_required INTEGER NOT NULL DEFAULT 0")
        }
    }
    private val migration_47_48 = object : Migration(47, 48) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE task_item_photos ADD COLUMN photo_date INTEGER NOT NULL DEFAULT 0")
        }
    }
    private val migration_48_53 = object : Migration(48, 53) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE `storage_report_photos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` TEXT NOT NULL, `report_id` INTEGER NOT NULL, `gps` TEXT NOT NULL, `time` INTEGER NOT NULL);")
            database.execSQL("CREATE TABLE `storage_report_query` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `storage_report_id` INTEGER NOT NULL, `task_id` INTEGER NOT NULL, `token` TEXT NOT NULL);")
            database.execSQL("CREATE TABLE `storage_reports` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `storage_id` INTEGER NOT NULL, `task_ids` TEXT NOT NULL, `gps` TEXT NOT NULL, `description` TEXT NOT NULL, `is_closed` INTEGER NOT NULL, `close_time` INTEGER, `battery_level` INTEGER, `device_radius` INTEGER, `device_close_any_distance` INTEGER, `device_allowed_distance` INTEGER, `is_photo_required` INTEGER);")
            database.execSQL("DROP TABLE tasks;")
            database.execSQL("DROP TABLE task_items;")
            database.execSQL("CREATE TABLE `tasks` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `edition` INTEGER NOT NULL, `copies` INTEGER NOT NULL, `packs` INTEGER NOT NULL, `remain` INTEGER NOT NULL, `area` INTEGER NOT NULL, `state` INTEGER NOT NULL, `start_time` INTEGER NOT NULL, `end_time` INTEGER NOT NULL, `brigade` INTEGER NOT NULL, `brigadier` TEXT NOT NULL, `rast_map_url` TEXT NOT NULL, `user_id` INTEGER NOT NULL, `city` TEXT NOT NULL, `iteration` INTEGER NOT NULL, `couple_type` INTEGER NOT NULL, `by_other_user` INTEGER NOT NULL, `delivery_type` INTEGER NOT NULL, `listSort` TEXT NOT NULL, `districtType` INTEGER NOT NULL, `orderNumber` INTEGER NOT NULL, `edition_photo_url` TEXT, `storage_close_first_required` INTEGER NOT NULL, `storage_id` INTEGER NOT NULL, `storage_address` TEXT NOT NULL, `storage_lat` REAL NOT NULL, `storage_long` REAL NOT NULL, `storage_close_distance` INTEGER NOT NULL, `closes` TEXT NOT NULL, `storage_photo_required` INTEGER NOT NULL, `storage_requirements_update_date` INTEGER NOT NULL, `storage_description` TEXT NOT NULL, PRIMARY KEY(`id`));\n")
            database.execSQL("CREATE TABLE `task_items` (`address_id` INTEGER NOT NULL, `state` INTEGER NOT NULL, `id` INTEGER NOT NULL, `notes` TEXT NOT NULL, `entrances` TEXT NOT NULL, `subarea` INTEGER NOT NULL, `bypass` INTEGER NOT NULL, `copies` INTEGER NOT NULL, `task_id` INTEGER NOT NULL, `need_photo` INTEGER NOT NULL, `is_firm` INTEGER NOT NULL, `office_name` TEXT NOT NULL, `firm_name` TEXT NOT NULL, `close_radius` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`task_id`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );")
        }
    }
}
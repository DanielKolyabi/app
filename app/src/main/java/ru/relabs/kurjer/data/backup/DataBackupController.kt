package ru.relabs.kurjer.data.backup

import android.content.Context
import android.os.Environment
import android.util.Log
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.domain.providers.RoomBackupProvider
import ru.relabs.kurjer.domain.storage.AppPreferences
import java.io.File

class DataBackupController(
    private val appPreferences: AppPreferences,
    private val provider: RoomBackupProvider,
    private val context: Context,
    private val db: AppDatabase
) {
    private val dbBackupFile = File(Environment.getDataDirectory(), "/databasebackup/deliverymanBackup.sqlite3").apply { mkdirs() }

    fun backup() {

        provider.roomBackup?.let {
            it.database(db)
                .enableLogDebug(true)
                .backupIsEncrypted(true)
                .customEncryptPassword(SECRET_PASSWORD)
                .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_FILE)
                .backupLocationCustomFile(dbBackupFile)
                .maxFileCount(2)
                .apply {
                    onCompleteListener { success, message, exitCode ->
                        Log.d("DatabaseBackup", "success: $success, message: $message, exitCode: $exitCode")
                    }
                }
                .backup()

        }

    }

    fun restore() {

        provider.roomBackup?.let {
            it.database(db)
                .enableLogDebug(true)
                .backupIsEncrypted(true)
                .customEncryptPassword(SECRET_PASSWORD)
                .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_FILE)
                .backupLocationCustomFile(dbBackupFile)
                .maxFileCount(2)
                .apply {
                    onCompleteListener { success, message, exitCode ->
                        Log.d("DatabaseBackup", "success: $success, message: $message, exitCode: $exitCode")
                    }
                }
                .restore()
        }
    }

    companion object {
        const val SECRET_PASSWORD = "relabs_deliveryman_very_secret_encryption_key"
    }
}
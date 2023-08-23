package ru.relabs.kurjer.data.backup

import android.content.Context
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import ru.relabs.kurjer.data.database.AppDatabase

class DataBackupController(val context: Context, db: AppDatabase) {

    private val databaseBackup = RoomBackup(context).database(db)
        .enableLogDebug(true)
        .backupIsEncrypted(true)
        .customEncryptPassword(SECRET_PASSWORD)
        .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_FILE)
        .maxFileCount(2)
        .apply {  }

    companion object {
        private const val SECRET_PASSWORD = "relabs_deliveryman_very_secret_encryption_key"
    }
}
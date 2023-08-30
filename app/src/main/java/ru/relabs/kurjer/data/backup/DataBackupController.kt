package ru.relabs.kurjer.data.backup

import android.content.Context
import android.os.Environment
import android.util.Log
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.domain.models.Credentials
import ru.relabs.kurjer.domain.providers.RoomBackupProvider
import ru.relabs.kurjer.domain.storage.SavedUserStorage
import ru.relabs.kurjer.utils.Either
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class DataBackupController(
    private val savedUserStorage: SavedUserStorage,
    private val provider: RoomBackupProvider,
    private val context: Context,
    private val db: AppDatabase
) {
    private val backupDir = File(Environment.getExternalStorageDirectory(), "databasebackup").apply { mkdirs() }
    private val dbFile = File(backupDir, "deliverymanBackup.sqlite3").apply { createNewFile() }
    private val credentialsFile = File(backupDir, "credentialsBackup").apply { createNewFile() }
    private val tokenFile = File(backupDir, "tokenBackup").apply { createNewFile() }

    fun backup() {
        try {
            backupDatabase()
            backupPreferences()
        } catch (e: Exception) {
            e.stackTrace.forEach { Log.e("DatabaseBackup", "$it") }
        }
    }

    fun restore(): Either<Exception, Unit> = Either.of {
        restoreDatabase()
        restorePreferences()
    }

    private fun backupDatabase() {
        provider.roomBackup?.let {
            it.database(db)
                .enableLogDebug(true)
                .backupIsEncrypted(true)
                .customEncryptPassword(SECRET_PASSWORD)
                .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_FILE)
                .backupLocationCustomFile(dbFile)
                .maxFileCount(2)
                .apply {
                    onCompleteListener { success, message, exitCode ->
                        Log.d("DatabaseBackup", "success: $success, message: $message, exitCode: $exitCode")
                    }
                }
                .backup()
        }
    }

    private fun backupPreferences() {
        Log.d("DatabaseBackup", "started")
        val savedCredentials = savedUserStorage.getCredentials()
        val savedToken = savedUserStorage.getToken()
        if (savedCredentials != null && savedToken != null) {
            val cos = ObjectOutputStream(credentialsFile.outputStream())
            cos.writeObject(savedCredentials)
            cos.close()
            val tos = ObjectOutputStream(tokenFile.outputStream())
            tos.writeObject(savedToken)
            tos.close()
        }
        Log.d("DatabaseBackup", "finished")
    }

    private fun restoreDatabase() {
        provider.roomBackup?.let {
            it.database(db)
                .enableLogDebug(true)
                .backupIsEncrypted(true)
                .customEncryptPassword(SECRET_PASSWORD)
                .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_FILE)
                .backupLocationCustomFile(dbFile)
                .maxFileCount(2)
                .apply {
                    onCompleteListener { success, message, exitCode ->
                        Log.d("DatabaseBackup", "success: $success, message: $message, exitCode: $exitCode")
                    }
                }
                .restore()
        }
    }

    private fun restorePreferences() {
        val cis = ObjectInputStream(credentialsFile.inputStream())
        val credentials = cis.readObject() as Credentials
        cis.close()
        Log.d("DatabaseBackup", "Credentials restored. login: ${credentials.login.login}, password: ${credentials.password}")
        val tis = ObjectInputStream(tokenFile.inputStream())
        val token = tis.readObject() as String
        Log.d("DatabaseBackup", "Token restored. token: $token")
        savedUserStorage.saveCredentials(credentials)
        savedUserStorage.saveToken(token)
    }


    companion object {
        const val SECRET_PASSWORD = "relabs_deliveryman_very_secret_encryption_key"
    }
}
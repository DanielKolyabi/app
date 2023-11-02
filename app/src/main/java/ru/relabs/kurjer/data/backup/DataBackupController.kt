package ru.relabs.kurjer.data.backup

import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.data.backup.roomBackup.RoomBackup
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.domain.models.Credentials
import ru.relabs.kurjer.domain.models.DeviceId
import ru.relabs.kurjer.domain.models.GpsRefreshTimes
import ru.relabs.kurjer.domain.providers.FirebaseToken
import ru.relabs.kurjer.domain.providers.PathsProvider
import ru.relabs.kurjer.domain.providers.RoomBackupProvider
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.domain.repositories.SettingsRepository
import ru.relabs.kurjer.domain.storage.AppPreferences
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.domain.storage.CurrentUserStorage
import ru.relabs.kurjer.domain.storage.SavedUserStorage
import ru.relabs.kurjer.utils.Either
import ru.relabs.kurjer.utils.Left
import ru.relabs.kurjer.utils.Right
import ru.relabs.kurjer.utils.extensions.checkedCreateFile
import ru.relabs.kurjer.utils.extensions.checkedMkDirs
import timber.log.Timber
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

class DataBackupController(
    private val appPreferences: AppPreferences,
    private val savedUserStorage: SavedUserStorage,
    private val currentUserStorage: CurrentUserStorage,
    private val authTokenStorage: AuthTokenStorage,
    private val settingsRepository: SettingsRepository,
    private val pauseRepository: PauseRepository,
    private val provider: RoomBackupProvider,
    private val db: AppDatabase,
    private val pathsProvider: PathsProvider,
    private val filesRootDir: File
) {

    private val backupDir = File(Environment.getExternalStorageDirectory(), "databasebackup")
        get() = field.apply { checkedMkDirs() }
    private val filesDir = File(backupDir, "internalFiles")
        get() = field.apply { checkedMkDirs() }
    private val dbFile = File(backupDir, "deliverymanBackup.sqlite3")
        get() = field.apply { checkedCreateFile() }
    private val credentialsFile = File(backupDir, "credentialsBackup")
        get() = field.apply { checkedCreateFile() }
    private val tokenFile = File(backupDir, "tokenBackup")
        get() = field.apply { checkedCreateFile() }
    private val preferencesFile = File(backupDir, "preferencesBackup")
        get() = field.apply { checkedCreateFile() }

    val backupExists = dbFile.exists() && filesDir.exists() && credentialsFile.exists() && tokenFile.exists()

    suspend fun startBackup() {
        withContext(Dispatchers.IO) {
            Timber.d("Scope launched")
            while (true) {
                delay(1000 * 60 * 10)
                Timber.d("Scope works")
                if (currentUserStorage.getCurrentUserLogin() != null && authTokenStorage.getToken() != null) {
                    when (val r = backup()) {
                        is Right -> {
                            Timber.d("BackupSucceed")
                        }

                        is Left -> {
                            Timber.d(r.value.message ?: "")
                            r.value.stackTrace.forEach {
                                Timber.d("$it")
                            }
                        }
                    }
                } else {
                    Timber.d("No user logged in")
                }
            }
        }
    }


    fun backup(): Either<Exception, Unit> = Either.of {
        backupDatabase()
        backupInternalFiles()
        backupPreferences()
    }

    fun restore(): Either<Exception, Unit> = Either.of {
        restoreDatabase()
        restorePreferences()
        restoreInternalFiles()
    }

    private fun backupDatabase() {
        provider.roomBackup?.also {
            it.database(db)
                .enableLogDebug(true)
                .backupIsEncrypted(true)
                .customEncryptPassword(SECRET_PASSWORD)
                .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_FILE)
                .backupLocationCustomFile(dbFile)
                .maxFileCount(2)
                .apply {
                    onCompleteListener { success, message, exitCode ->
                        Timber.d("success: $success, message: $message, exitCode: $exitCode")
                    }
                }
                .backup()
        }
    }

    private fun restoreDatabase() {
        provider.roomBackup?.also {
            it.database(db)
                .enableLogDebug(true)
                .backupIsEncrypted(true)
                .customEncryptPassword(SECRET_PASSWORD)
                .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_FILE)
                .backupLocationCustomFile(dbFile)
                .maxFileCount(2)
                .apply {
                    onCompleteListener { success, message, exitCode ->
                        Timber.d("DataBackupController", "success: $success, message: $message, exitCode: $exitCode")
                    }
                }
                .restore()
        }
    }

    private fun backupPreferences() {
        savedUserStorage.getCredentials()?.let { saveToFile(it, credentialsFile) }
        savedUserStorage.getToken()?.let { saveToFile(it, tokenFile) }
        val (lunchLastStart, loadLastStart) = pauseRepository.getStartTimes()
        val (lunchLastEnd, loadLastEnd) = pauseRepository.getEndTimes()
        val (lunchDuration, loadDuration) = pauseRepository.getPauseDurations()
        val preferences =
            BackupPreferences(
                appPreferences.getDeviceUUID() ?: DeviceId(""),
                appPreferences.getFirebaseToken() ?: FirebaseToken(""),
                settingsRepository.loadSavedGPSRefreshTimes(),
                settingsRepository.isCloseRadiusRequired,
                settingsRepository.isPhotoRadiusRequired,
                settingsRepository.isStorageCloseRadiusRequired,
                settingsRepository.isStoragePhotoRadiusRequired,
                settingsRepository.canSkipUpdates,
                settingsRepository.canSkipUnfinishedTaskItem,
                lunchLastStart,
                loadLastStart,
                lunchLastEnd,
                loadLastEnd,
                lunchDuration,
                loadDuration,
            )
        saveToFile(preferences, preferencesFile)
        Timber.d("Preferences backed up")
    }

    private fun <T> saveToFile(source: T, destinationFile: File) {
        val stream = ObjectOutputStream(destinationFile.outputStream())
        stream.writeObject(source)
        stream.close()
    }

    private fun restorePreferences() {
        val credentials = fromFile(credentialsFile) { it as Credentials }
        Timber.d("Credentials restored. login: ${credentials.login.login}, password: ${credentials.password}")
        val token = fromFile(tokenFile) { it as String }
        Timber.d("Token restored. token: $token")
        savedUserStorage.saveCredentials(credentials)
        savedUserStorage.saveToken(token)
        val preferences = fromFile(preferencesFile) { it as BackupPreferences }
        Timber.d("$preferences restored")
        appPreferences.saveDeviceUUID(preferences.deviceId)
        appPreferences.saveFirebaseToken(preferences.firebaseToken)
        settingsRepository.saveFromRestored(
            preferences.gpsRefreshTimes,
            preferences.isCloseRadiusRequired,
            preferences.isPhotoRadiusRequired,
            preferences.isStorageCloseRadiusRequired,
            preferences.isStoragePhotoRadiusRequired,
            preferences.canSkipUpdates,
            preferences.canSkipUnfinishedTaskItem
        )
        pauseRepository.putRestoredData(
            preferences.lunchLastStart,
            preferences.loadLastStart,
            preferences.lunchLastEnd,
            preferences.loadLastEnd,
            preferences.lunchDuration,
            preferences.loadDuration,
        )

    }

    private fun <T> fromFile(sourceFile: File, mapper: (Any) -> T): T {
        val stream = ObjectInputStream(sourceFile.inputStream())
        return mapper(stream.readObject()).also { stream.close() }
    }

    private fun backupInternalFiles() {
        pathsProvider.getDirectories().forEach {
            it.copyRecursively(File(filesDir, it.nameWithoutExtension), true)
            Timber.d("File $it backup completed")
        }
        Timber.d("Files backup completed")
    }

    private fun restoreInternalFiles() {
        PathsProvider.getDirNames().forEach {
            File(filesDir, it).copyRecursively(File(filesRootDir, it), true)
        }

        Timber.d("Files restore completed")
    }

    companion object {
        private const val SECRET_PASSWORD = "relabs_deliveryman_very_secret_encryption_key"
    }
}

private data class BackupPreferences(
    val deviceId: DeviceId,
    val firebaseToken: FirebaseToken,
    val gpsRefreshTimes: GpsRefreshTimes,
    val isCloseRadiusRequired: Boolean,
    val isPhotoRadiusRequired: Boolean,
    val isStorageCloseRadiusRequired: Boolean,
    val isStoragePhotoRadiusRequired: Boolean,
    val canSkipUpdates: Boolean,
    val canSkipUnfinishedTaskItem: Boolean,
    val lunchLastStart: Long,
    val loadLastStart: Long,
    val lunchLastEnd: Long,
    val loadLastEnd: Long,
    val lunchDuration: Int,
    val loadDuration: Int
) : Serializable
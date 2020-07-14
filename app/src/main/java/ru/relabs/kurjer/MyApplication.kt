package ru.relabs.kurjer

import android.annotation.SuppressLint
import android.app.Application
import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Room
import android.arch.persistence.room.migration.Migration
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaDrm
import android.os.Build
import android.os.StrictMode
import android.support.v4.content.ContextCompat
import android.telephony.TelephonyManager
import com.crashlytics.android.Crashlytics
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.firebase.iid.FirebaseInstanceId
import com.instacart.library.truetime.TrueTime
import com.yandex.mapkit.MapKitFactory
import io.fabric.sdk.android.Fabric
import kotlinx.coroutines.experimental.launch
import ru.relabs.kurjer.models.GPSCoordinatesModel
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.network.DeliveryServerAPI
import ru.relabs.kurjer.persistence.AppDatabase
import ru.relabs.kurjer.repository.LocationProvider
import ru.relabs.kurjer.repository.PauseRepository
import ru.relabs.kurjer.repository.RadiusRepository
import ru.relabs.kurjer.repository.getLocationProvider
import ru.relabs.kurjer.utils.CustomLog
import ru.relabs.kurjer.utils.instanceIdAsync
import ru.relabs.kurjer.utils.tryOrLogAsync
import java.util.*


/**
 * Created by ProOrange on 30.08.2018.
 */

class MyApplication : Application() {
    lateinit var pauseRepository: PauseRepository
    lateinit var radiusRepository: RadiusRepository
    lateinit var database: AppDatabase
    var user: UserModel = UserModel.Unauthorized
    lateinit var deviceUUID: String
    var locationManager: FusedLocationProviderClient? = null
    var currentLocation = GPSCoordinatesModel(0.0, 0.0, Date(0))
    lateinit var locationProvider: LocationProvider

    var lastRequiredAppVersion = 0

    val listener = object : LocationCallback() {

        override fun onLocationResult(location: LocationResult?) {
            location?.let {
                currentLocation = GPSCoordinatesModel(it.lastLocation.latitude, it.lastLocation.longitude, Date(it.lastLocation.time))
            }
        }
    }

    @SuppressLint("HardwareIds")
    fun getDeviceUniqueId(): String {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val imei = try {
            when {
                Build.VERSION.SDK_INT >= 29 -> {
                    val WIDEVINE_UUID = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
                    val id = MediaDrm(WIDEVINE_UUID).getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
                    Base64.getEncoder().encodeToString(id)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                    telephonyManager.getImei(0) ?: telephonyManager.getImei(1) ?: ""

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    telephonyManager.getDeviceId(0) ?: (telephonyManager.getDeviceId(1) ?: "")

                else ->
                    telephonyManager.deviceId
            }
        } catch (e: SecurityException) {
            CustomLog.writeToFile("IMEI: Stack \n" + CustomLog.getStacktraceAsString(e))
            ""
        }

        return imei
    }

    override fun onCreate() {
        super.onCreate()

        launch{
            TrueTime.build().withSharedPreferencesCache(this@MyApplication).initialize()
        }
        Fabric.with(this, Crashlytics())

        locationManager = FusedLocationProviderClient(applicationContext)
        instance = this

        MapKitFactory.setApiKey(BuildConfig.YA_KEY)

        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().build())
        deviceUUID = getOrGenerateDeviceUUID()

        val migration_26_27 = object : Migration(26, 27) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE task_items ADD COLUMN need_photo INTEGER NOT NULL DEFAULT 0")
            }
        }
        val migration_27_28 = object : Migration(27, 28) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE report_query ADD COLUMN battery_level INTEGER NOT NULL DEFAULT 0")
            }
        }
        val migration_28_29 = object : Migration(28, 29) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
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
                """.trimIndent())
            }
        }
        val migration_29_30 = object : Migration(29, 30) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN couple_type INTEGER NOT NULL DEFAULT 1")
            }
        }
        val migration_30_31 = object : Migration(30, 31) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE report_query ADD COLUMN remove_after_send INTEGER NOT NULL DEFAULT 0")
            }
        }
        val migration_31_32 = object : Migration(31, 32) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE report_query ADD COLUMN close_distance INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE report_query ADD COLUMN allowed_distance INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE report_query ADD COLUMN radius_required INTEGER NOT NULL DEFAULT 0")
            }
        }
        val migration_32_33 = object : Migration(32, 33) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE task_item_photos ADD COLUMN entrance_number INTEGER NOT NULL DEFAULT -1")
            }
        }
        val migration_33_34 = object : Migration(33, 34) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE entrances_data ADD COLUMN photo_required INTEGER NOT NULL DEFAULT 0")
            }
        }

        database = Room
                .databaseBuilder(applicationContext, AppDatabase::class.java, "deliveryman")
                .addMigrations(migration_26_27, migration_27_28, migration_28_29,
                        migration_29_30, migration_30_31, migration_31_32, migration_32_33,
                        migration_33_34)
                .build()

        pauseRepository = PauseRepository(
                DeliveryServerAPI.api,
                getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE),
                database
        ) { (user as? UserModel.Authorized)?.token }
        radiusRepository = RadiusRepository(
                DeliveryServerAPI.api,
                getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
        ) { (user as? UserModel.Authorized)?.token }
        locationProvider = getLocationProvider(this)
    }

    fun requestLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        locationManager?.lastLocation?.addOnSuccessListener { location ->
            location?.let {
                currentLocation = GPSCoordinatesModel(it.latitude, it.longitude, Date(it.time))
            }
        }
    }

    fun enableLocationListening(time: Long = 60 * 1000, distance: Float = 10f): Boolean {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        val req = LocationRequest().apply {
            fastestInterval = 10 * 1000
            interval = time

            priority = PRIORITY_HIGH_ACCURACY
        }

        locationManager?.requestLocationUpdates(req, listener, mainLooper)

        return true
    }

    fun storeUserCredentials() {
        if (user !is UserModel.Authorized) return
        getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
                .edit()
                .putString("login", (user as UserModel.Authorized).login)
                .putString("token", (user as UserModel.Authorized).token)
                .apply()
    }

    fun getUserCredentials(): UserModel.Authorized? {
        val login = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).getString("login", "-unknw")
        val token = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).getString("token", "-unknw")
        if (token == "-unknw") {
            return null
        }
        return UserModel.Authorized(login = login, token = token)
    }

    fun restoreUserCredentials() {
        getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
                .edit()
                .remove("login")
                .remove("token")
                .apply()
    }


    fun getOrGenerateDeviceUUID(): String {
        val sharedPreferences = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
        var deviceUUID = sharedPreferences.getString(
                "device_uuid", "unknown"
        )

        if (deviceUUID == "unknown") {
            deviceUUID = UUID.randomUUID().toString()
            sharedPreferences.edit()
                    .putString("device_uuid", deviceUUID)
                    .apply()
        }
        return deviceUUID
    }

    fun savePushToken(pushToken: String) {
        getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
                .edit()
                .putString("firebase_token", pushToken)
                .apply()

    }

    suspend fun sendDeviceInfo(pushToken: String?, shouldSendImei: Boolean = true) {
        if (user !is UserModel.Authorized) return

        if (shouldSendImei) {
            tryOrLogAsync {
                DeliveryServerAPI.api.sendDeviceImei((user as UserModel.Authorized).token, getDeviceUniqueId()).await()
            }
        }

        if (pushToken != null) {
            tryOrLogAsync {
                DeliveryServerAPI.api.sendPushToken((user as UserModel.Authorized).token, pushToken).await()
            }
        } else {
            val token = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).getString("firebase_token", "notoken")
            if (token != "notoken") {
                sendDeviceInfo(token)
                return
            }

            tryOrLogAsync {
                val token = FirebaseInstanceId.getInstance().instanceIdAsync().token
                savePushToken(token)
                sendDeviceInfo(token, false)
            }
        }
    }

    companion object {
        lateinit var instance: MyApplication
    }
}
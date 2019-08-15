package ru.relabs.kurjer

import android.annotation.SuppressLint
import android.app.Application
import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Room
import android.arch.persistence.room.migration.Migration
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.StrictMode
import android.support.v4.content.ContextCompat
import android.telephony.TelephonyManager
import com.crashlytics.android.Crashlytics
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.firebase.iid.FirebaseInstanceId
import com.yandex.mapkit.MapKitFactory
import io.fabric.sdk.android.Fabric
import ru.relabs.kurjer.models.GPSCoordinatesModel
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.network.DeliveryServerAPI
import ru.relabs.kurjer.persistence.AppDatabase
import java.util.*


/**
 * Created by ProOrange on 30.08.2018.
 */

class MyApplication : Application() {
    lateinit var database: AppDatabase
    var user: UserModel = UserModel.Unauthorized
    lateinit var deviceUUID: String
    var locationManager: FusedLocationProviderClient? = null
    var currentLocation = GPSCoordinatesModel(0.0, 0.0, Date(0))

    var lastRequiredAppVersion = 0

    val listener = object : LocationCallback() {

        override fun onLocationResult(location: LocationResult?) {
            location?.let {
                currentLocation = GPSCoordinatesModel(it.lastLocation.latitude, it.lastLocation.longitude, Date(it.lastLocation.time))
            }
        }
    }

    @SuppressLint("HardwareIds")
    fun getImei(): String {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val imei = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telephonyManager.getImei(0) ?: telephonyManager.getImei(1) ?: ""
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                telephonyManager.getDeviceId(0) ?: (telephonyManager.getDeviceId(1) ?: "")
            } else {
                telephonyManager.deviceId
            }
        } catch (e: SecurityException) {
            ""
        }

        return imei
    }

    override fun onCreate() {
        super.onCreate()

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

        database = Room
                .databaseBuilder(applicationContext, ru.relabs.kurjer.persistence.AppDatabase::class.java, "deliveryman")
                .addMigrations(migration_26_27, migration_27_28, migration_28_29,
                        migration_29_30)
                .build()
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

    fun enableLocationListening(time: Long = 30 * 1000, distance: Float = 10f): Boolean {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        val req = LocationRequest().apply {
            fastestInterval = time
            smallestDisplacement = distance
            priority = PRIORITY_BALANCED_POWER_ACCURACY
        }

        locationManager?.requestLocationUpdates(req, listener, mainLooper)

        return true
    }

    fun disableLocationListening() {
//        locationManager?.removeLocationUpdates(listener)
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

    fun sendDeviceInfo(pushToken: String?, shouldSendImei: Boolean = true) {
        if (user !is UserModel.Authorized) return

        if (shouldSendImei) {
            tryOrLog {
                DeliveryServerAPI.api.sendDeviceImei((user as UserModel.Authorized).token, getImei())
            }
        }

        if (pushToken != null) {
            tryOrLog {
                DeliveryServerAPI.api.sendPushToken((user as UserModel.Authorized).token, pushToken)
            }
        } else {
            val token = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).getString("firebase_token", "notoken")
            if (token != "notoken") {
                sendDeviceInfo(token)
                return
            }

            FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
                savePushToken(it.token)
                sendDeviceInfo(it.token, false)
            }
        }
    }

    companion object {
        lateinit var instance: MyApplication
    }
}
package ru.relabs.kurjer

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaDrm
import android.os.Build
import android.os.StrictMode
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.instacart.library.truetime.TrueTime
import com.yandex.mapkit.MapKitFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.relabs.kurjer.di.*
import ru.relabs.kurjer.domain.models.User
import ru.relabs.kurjer.models.GPSCoordinatesModel
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.utils.CustomLog
import ru.terrakok.cicerone.Cicerone
import ru.terrakok.cicerone.NavigatorHolder
import ru.terrakok.cicerone.Router
import java.util.*


/**
 * Created by ProOrange on 30.08.2018.
 */

class DeliveryApp : Application() {
    private val scope = CoroutineScope(Dispatchers.Default)

    private lateinit var cicerone: Cicerone<Router>

    val router: Router
        get() = cicerone.router
    val navigatorHolder: NavigatorHolder
        get() = cicerone.navigatorHolder


    var user: User? = null
    var locationManager: FusedLocationProviderClient? = null
    var currentLocation = GPSCoordinatesModel(0.0, 0.0, Date(0))

    var lastRequiredAppVersion = 0

    val listener = object : LocationCallback() {

        override fun onLocationResult(location: LocationResult?) {
            location?.let {
                currentLocation = GPSCoordinatesModel(
                    it.lastLocation.latitude,
                    it.lastLocation.longitude,
                    Date(it.lastLocation.time)
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        MapKitFactory.setApiKey(BuildConfig.YA_KEY)

        cicerone = Cicerone.create()

        launchTrueTimeInit()

        startKoin {
            androidContext(this@DeliveryApp)
            modules(listOf(constModule, navigationModule, fileSystemModule, storagesModule, repositoryModule, useCasesModule))
        }
        //All below is a trash

        locationManager = FusedLocationProviderClient(applicationContext)

        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().build())
    }

    private fun launchTrueTimeInit() {
        scope.launch(Dispatchers.IO) {
            while (!initTrueTime()) {
                delay(500)
            }
            CustomLog.writeToFile("True Time initialized")
        }
    }

    @SuppressLint("HardwareIds")
    fun getDeviceUniqueId(): String {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val imei = try {
            when {
                Build.VERSION.SDK_INT >= 29 -> {
                    val WIDEVINE_UUID = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
                    val id =
                        MediaDrm(WIDEVINE_UUID).getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
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

    private suspend fun initTrueTime(): Boolean {
        try {
            TrueTime.build().withSharedPreferencesCache(this@DeliveryApp).initialize()
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun requestLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        locationManager?.lastLocation?.addOnSuccessListener { location ->
            location?.let {
                currentLocation = GPSCoordinatesModel(it.latitude, it.longitude, Date(it.time))
            }
        }
    }

    fun enableLocationListening(time: Long = 60 * 1000, distance: Float = 10f): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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
        val ctxUser = user ?: return
        getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
            .edit()
            .putString("login", ctxUser.login.login)
            //.putString("token", (user as UserModel.Authorized).token)
            .apply()
    }

    fun getUserCredentials(): UserModel.Authorized? {
        val login = getSharedPreferences(
            BuildConfig.APPLICATION_ID,
            Context.MODE_PRIVATE
        ).getString("login", "-unknw") ?: "-unknw"
        val token = getSharedPreferences(
            BuildConfig.APPLICATION_ID,
            Context.MODE_PRIVATE
        ).getString("token", "-unknw") ?: "-unknw"
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
        val sharedPreferences =
            getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
        var deviceUUID = sharedPreferences.getString("device_uuid", "unknown") ?: "unknown"

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
        if (user == null) return

        TODO("Refactor")
//        if (shouldSendImei) {
//            tryOrLogAsync {
//                DeliveryServerAPI.api.sendDeviceImei(
//                    (user as UserModel.Authorized).token,
//                    getDeviceUniqueId()
//                )
//            }
//        }
//
//        if (pushToken != null) {
//            tryOrLogAsync {
//                DeliveryServerAPI.api.sendPushToken((user as UserModel.Authorized).token, pushToken)
//
//            }
//        } else {
//            val token = getSharedPreferences(
//                BuildConfig.APPLICATION_ID,
//                Context.MODE_PRIVATE
//            ).getString("firebase_token", "notoken")
//            if (token != "notoken") {
//                sendDeviceInfo(token)
//                return
//            }
//
//            tryOrLogAsync {
//                val token = FirebaseInstanceId.getInstance().instanceIdAsync().token
//                savePushToken(token)
//                sendDeviceInfo(token, false)
//            }
//        }
    }

    companion object {
        lateinit var appContext: Context
    }
}
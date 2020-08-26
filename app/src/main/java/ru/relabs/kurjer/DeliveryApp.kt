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
            modules(
                listOf(
                    constModule,
                    navigationModule,
                    fileSystemModule,
                    eventControllers,
                    storagesModule,
                    repositoryModule,
                    useCasesModule
                )
            )
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

    private suspend fun initTrueTime(): Boolean {
        return try {
            TrueTime.build().withSharedPreferencesCache(this@DeliveryApp).initialize()
            true
        } catch (e: Exception) {
            false
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

    companion object {
        lateinit var appContext: Context
    }
}
package ru.relabs.kurjer.domain.providers

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import ru.relabs.kurjer.uiOld.helpers.formatedWithSecs
import ru.relabs.kurjer.utils.CustomLog
import java.util.*

/**
 * Created by Daniil Kurchanov on 13.01.2020.
 */
interface LocationProvider {
    val location: Flow<Location?>

    fun updatesChannel(fastest: Boolean = false): ReceiveChannel<Location>
    fun lastReceivedLocation(): Location?

    fun startInBackground(): Boolean
    fun stopInBackground()
}

@ExperimentalCoroutinesApi
class PlayServicesLocationProvider(
    private val client: FusedLocationProviderClient,
    private val application: Application,
    private val mainHandlerScope: CoroutineScope
) : LocationProvider {
    override val location: MutableStateFlow<Location?> = MutableStateFlow(null)

    private var subscribedChannels = 0
    private var subscribedBackgrounds = 0


    private var lastReceivedLocation: Location? = null
        get() = field
        set(v) {
            location.value = v
            field = v
        }

    private val backgroundCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            CustomLog.writeToFile("GPS LOG: BG Receive ${result.lastLocation?.let { Date(it.time).formatedWithSecs() }}")
            lastReceivedLocation = result.lastLocation
        }
    }
    private var isBackgroundRunning = false

    override fun lastReceivedLocation(): Location? = lastReceivedLocation

    override fun updatesChannel(fastest: Boolean): ReceiveChannel<Location> {
        val channel = Channel<Location>(Channel.UNLIMITED)

        CustomLog.writeToFile("GPS LOG: Forced GPS request")
        if (!checkPermission()) {
            CustomLog.writeToFile("GPS LOG: No permission")
            lastReceivedLocation?.let {
                channel.trySend(it).isSuccess
            }
            return channel
        }

        if (fastest) {
            client.lastLocation.addOnSuccessListener { location: Location? ->
                if (!channel.isClosedForSend) {
                    location?.let {
                        CustomLog.writeToFile("GPS LOG: Fastest method, ${Date(location.time).formatedWithSecs()}")
                        channel.trySend(it).isSuccess
                    }
                }
            }
        }
        val request = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            fastestInterval = 1000
            interval = 5000
        }
        val callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (!channel.isClosedForSend) {
                    val lastLocation = locationResult.lastLocation
                    val location = locationResult.locations.firstOrNull()
                    CustomLog.writeToFile(
                        "GPS LOG: Normal flow location: ${Date(location?.time ?: 0).formatedWithSecs()}, " +
                                "lastLocation: ${Date(lastLocation?.time ?: 0).formatedWithSecs()}"
                    )
                    if (location != null) {
                        channel.trySend(location)
                    } else if (lastLocation != null) {
                        channel.trySend(lastLocation).isSuccess
                    }
                }
                lastReceivedLocation = locationResult.lastLocation
            }
        }
        val shouldRunBackgroundAfter = isBackgroundRunning
        if (isBackgroundRunning) {
            stopInBackground()
        }
        mainHandlerScope.launch(Dispatchers.Main) {
            subscribedChannels++
            CustomLog.writeToFile("GPS LOG: Subscribe $callback ($subscribedChannels)")
            client.requestLocationUpdates(request, callback, null)
        }
        channel.invokeOnClose {
            mainHandlerScope.launch(Dispatchers.Main) {
                subscribedChannels--
                CustomLog.writeToFile("GPS LOG: Unsubscribe $callback ($subscribedChannels)")
                client.removeLocationUpdates(callback)
            }
            if (shouldRunBackgroundAfter) {
                startInBackground()
            }
        }
        return channel
    }

    override fun startInBackground(): Boolean {
        if (isBackgroundRunning) {
            return true
        }
        if (!checkPermission()) {
            return false
        }
        CustomLog.writeToFile("GPS LOG: Start In Background")
        val request = LocationRequest().apply {
            fastestInterval = 10 * 1000
            interval = 60 * 1000

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        isBackgroundRunning = true
        mainHandlerScope.launch(Dispatchers.Main) {
            subscribedBackgrounds++
            CustomLog.writeToFile("GPS LOG: Subscribe BG $backgroundCallback (${subscribedBackgrounds})")
            client.requestLocationUpdates(request, backgroundCallback, null)
        }
        return true
    }

    override fun stopInBackground() {
        if (!isBackgroundRunning) return
        CustomLog.writeToFile("GPS LOG: Stop BG")
        isBackgroundRunning = false
        mainHandlerScope.launch(Dispatchers.Main) {
            subscribedBackgrounds--
            CustomLog.writeToFile("GPS LOG: Unsubscribe BG $backgroundCallback (${subscribedBackgrounds})")
            client.removeLocationUpdates(backgroundCallback)
        }
    }

    private fun checkPermission(): Boolean = application.let {
        ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}

@ExperimentalCoroutinesApi
class NativeLocationProvider(
    private val client: LocationManager,
    private val application: Application,
    private val mainHandlerScope: CoroutineScope
) : LocationProvider {
    override val location: MutableStateFlow<Location?> = MutableStateFlow(null)
    private var lastReceivedLocation: Location? = null
        get() = field
        set(v) {
            location.value = v
            field = v
        }

    private val backgroundCallback = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lastReceivedLocation = location
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }
    private var isBackgroundRunning = false

    override fun lastReceivedLocation(): Location? = lastReceivedLocation

    override fun updatesChannel(fastest: Boolean): ReceiveChannel<Location> {
        val channel = Channel<Location>(Channel.UNLIMITED)

        if (!checkPermission()) {
            lastReceivedLocation?.let {
                channel.trySend(it).isSuccess
            }
            return channel
        }

        if (fastest) {
            val loc = try {
                client.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: client.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (e: SecurityException) {
                null
            }

            loc?.let {
                channel.trySend(loc).isSuccess
            }
        }
        val callback: LocationListener = object : LocationListener {
            override fun onLocationChanged(locationResult: Location) {
                locationResult
                if (!channel.isClosedForSend) {
                    channel.trySend(locationResult).isSuccess
                }
                lastReceivedLocation = locationResult
            }

            override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
        }

        val shouldRunBackgroundAfter = isBackgroundRunning
        if (isBackgroundRunning) {
            stopInBackground()
        }
        mainHandlerScope.launch(Dispatchers.Main) {
            client.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, callback)
            client.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, callback)
        }

        channel.invokeOnClose {
            mainHandlerScope.launch(Dispatchers.Main) {
                client.removeUpdates(callback)
            }
            if (shouldRunBackgroundAfter) {
                startInBackground()
            }
        }
        return channel
    }

    override fun startInBackground(): Boolean {
        if (isBackgroundRunning) {
            return true
        }
        if (!checkPermission()) {
            return false
        }
        isBackgroundRunning = true

        mainHandlerScope.launch(Dispatchers.Main) {
            client.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10 * 1000, 10f, backgroundCallback)
            client.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10 * 1000, 10f, backgroundCallback)
        }
        return true
    }

    override fun stopInBackground() {
        if (!isBackgroundRunning) return
        isBackgroundRunning = false
        mainHandlerScope.launch(Dispatchers.Main) {
            client.removeUpdates(backgroundCallback)
        }
    }


    private fun checkPermission(): Boolean = application.let {
        ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}

fun getLocationProvider(application: Application, mainHandlerScope: CoroutineScope): LocationProvider {
    return if (GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(application.applicationContext) == ConnectionResult.SUCCESS
    ) {
        CustomLog.writeToFile("Used PlayServices provider")
        PlayServicesLocationProvider(
            LocationServices.getFusedLocationProviderClient(application),
            application,
            mainHandlerScope
        )
    } else {
        CustomLog.writeToFile("Used native provider")
        NativeLocationProvider(
            application.getSystemService(Context.LOCATION_SERVICE) as LocationManager,
            application,
            mainHandlerScope
        )
    }
}
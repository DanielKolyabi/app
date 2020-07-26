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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import ru.relabs.kurjer.utils.CustomLog

/**
 * Created by Daniil Kurchanov on 13.01.2020.
 */
interface LocationProvider {
    fun updatesChannel(fastest: Boolean = false): ReceiveChannel<Location>
    fun lastReceivedLocation(): Location?

    fun startInBackground(): Boolean
    fun stopInBackground()
}

@ExperimentalCoroutinesApi
class PlayServicesLocationProvider(
    private val client: FusedLocationProviderClient,
    private val application: Application
) :
    LocationProvider {
    private var lastReceivedLocation: Location? = null
    private val backgroundCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            lastReceivedLocation = result.lastLocation
        }
    }
    private var isBackgroundRunning = false

    override fun lastReceivedLocation(): Location? = lastReceivedLocation

    override fun updatesChannel(fastest: Boolean): ReceiveChannel<Location> {
        val channel = Channel<Location>(Channel.UNLIMITED)

        if (!checkPermission()) {
            lastReceivedLocation?.let {
                channel.offer(it)
            }
            return channel
        }

        if (fastest) {
            client.lastLocation.addOnSuccessListener { location: Location? ->
                if (!channel.isClosedForSend) {
                    location?.let {
                        channel.offer(it)
                    }
                }
            }
        }
        val request = LocationRequest.create()
        val callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (!channel.isClosedForSend) {
                    val lastLocation = locationResult.lastLocation
                    if (lastLocation != null) {
                        channel.offer(lastLocation)
                    } else {
                        locationResult.locations.firstOrNull()?.let {
                            channel.offer(it)
                        }
                    }
                }
                lastReceivedLocation = locationResult.lastLocation
            }
        }
        val shouldRunBackgroundAfter = isBackgroundRunning
        if (isBackgroundRunning) {
            stopInBackground()
        }
        client.requestLocationUpdates(request, callback, null)
        channel.invokeOnClose {
            client.removeLocationUpdates(callback)
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
        if (checkPermission()) {
            return false
        }
        val request = LocationRequest().apply {
            fastestInterval = 10 * 1000
            interval = 60 * 1000

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        isBackgroundRunning = true
        client.requestLocationUpdates(request, backgroundCallback, null)
        return true
    }

    override fun stopInBackground() {
        if (!isBackgroundRunning) return
        isBackgroundRunning = false
        client.removeLocationUpdates(backgroundCallback)
    }

    private fun checkPermission(): Boolean = application.let {
        ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
    }
}

@ExperimentalCoroutinesApi
class NativeLocationProvider(
    private val client: LocationManager,
    private val application: Application
) : LocationProvider {
    private var lastReceivedLocation: Location? = null
    private val backgroundCallback = object : LocationListener {
        override fun onLocationChanged(location: Location?) {
            lastReceivedLocation = location
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String?) {}
        override fun onProviderDisabled(provider: String?) {}
    }
    private var isBackgroundRunning = false

    override fun lastReceivedLocation(): Location? = lastReceivedLocation

    override fun updatesChannel(fastest: Boolean): ReceiveChannel<Location> {
        val channel = Channel<Location>(Channel.UNLIMITED)

        if (!checkPermission()) {
            lastReceivedLocation?.let {
                channel.offer(it)
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
                channel.offer(loc)
            }
        }
        val callback: LocationListener = object : LocationListener {
            override fun onLocationChanged(locationResult: Location?) {
                locationResult ?: return
                if (!channel.isClosedForSend) {
                    channel.offer(locationResult)
                }
                lastReceivedLocation = locationResult
            }

            override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
            override fun onProviderEnabled(p0: String?) {}
            override fun onProviderDisabled(p0: String?) {}
        }

        val shouldRunBackgroundAfter = isBackgroundRunning
        if (isBackgroundRunning) {
            stopInBackground()
        }
        client.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, callback)
        client.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, callback)

        channel.invokeOnClose {
            client.removeUpdates(callback)
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
        client.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10 * 1000, 10f, backgroundCallback)
        client.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10 * 1000, 10f, backgroundCallback)
        return true
    }

    override fun stopInBackground() {
        if (!isBackgroundRunning) return
        isBackgroundRunning = false
        client.removeUpdates(backgroundCallback)
    }

    private fun checkPermission(): Boolean = application.let {
        ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
    }
}

fun getLocationProvider(application: Application): LocationProvider {
    return if (GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(application.applicationContext) == ConnectionResult.SUCCESS
    ) {
        CustomLog.writeToFile("Used PlayServices provider")
        PlayServicesLocationProvider(
            LocationServices.getFusedLocationProviderClient(application),
            application
        )
    } else {
        CustomLog.writeToFile("Used native provider")
        NativeLocationProvider(
            application.getSystemService(Context.LOCATION_SERVICE) as LocationManager,
            application
        )
    }
}
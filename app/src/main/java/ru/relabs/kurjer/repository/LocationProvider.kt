package ru.relabs.kurjer.repository

import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import com.yandex.runtime.logging.Logger.debug
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import ru.relabs.kurjer.utils.CustomLog

/**
 * Created by Daniil Kurchanov on 13.01.2020.
 */
interface LocationProvider {
    fun updatesChannel(fastest: Boolean = false): ReceiveChannel<Location>
    fun lastReceivedLocation(): Location?
}

class PlayServicesLocationProvider(private val client: FusedLocationProviderClient) :
        LocationProvider {
    private var lastReceivedLocation: Location? = null

    override fun lastReceivedLocation(): Location? = lastReceivedLocation

    override fun updatesChannel(fastest: Boolean): ReceiveChannel<Location> {
        val channel = Channel<Location>(Channel.UNLIMITED)
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
        client.requestLocationUpdates(request, callback, null)
        channel.invokeOnClose { client.removeLocationUpdates(callback) }
        return channel
    }
}

class NativeLocationProvider(private val client: LocationManager) : LocationProvider {
    private var lastReceivedLocation: Location? = null

    override fun lastReceivedLocation(): Location? = lastReceivedLocation

    override fun updatesChannel(fastest: Boolean): ReceiveChannel<Location> {
        val channel = Channel<Location>(Channel.UNLIMITED)
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
        try {
            client.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, callback)
            client.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, callback)
        } catch (e: SecurityException) {
        }

        channel.invokeOnClose { client.removeUpdates(callback) }
        return channel
    }
}

fun getLocationProvider(application: Application): LocationProvider {
    return if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(application.applicationContext) == ConnectionResult.SUCCESS) {
        CustomLog.writeToFile("Used PlayServices provider")
        PlayServicesLocationProvider(LocationServices.getFusedLocationProviderClient(application))
    } else {
        CustomLog.writeToFile("Used native provider")
        NativeLocationProvider(application.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
    }
}
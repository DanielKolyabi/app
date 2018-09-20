package ru.relabs.kurjer

import android.app.Application
import android.arch.persistence.room.Room
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.StrictMode
import android.support.v4.content.ContextCompat
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.yandex.mapkit.MapKitFactory
import kotlinx.coroutines.experimental.launch
import ru.relabs.kurjer.models.GPSCoordinatesModel
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.network.DeliveryServerAPI
import ru.relabs.kurjer.persistence.AppDatabase
import ru.relabs.kurjer.persistence.entities.AddressEntity
import ru.relabs.kurjer.persistence.entities.TaskEntity
import ru.relabs.kurjer.persistence.entities.TaskItemEntity
import java.util.*

/**
 * Created by ProOrange on 30.08.2018.
 */

class MyApplication : Application() {
    lateinit var database: AppDatabase
    var user: UserModel = UserModel.Unauthorized
    lateinit var deviceUUID: String
    private lateinit var locationManager: LocationManager
    var currentLocation = GPSCoordinatesModel(0.0, 0.0, Date(0))

    override fun onCreate() {
        super.onCreate()

        MapKitFactory.setApiKey(BuildConfig.YA_KEY);

        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().build())
        deviceUUID = getOrGenerateDeviceUUID()

        database = Room
                .databaseBuilder(applicationContext, ru.relabs.kurjer.persistence.AppDatabase::class.java, "deliveryman")
                .fallbackToDestructiveMigration()
                .build()
    }

    fun enableLocationListening() : Boolean{
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location?) {
                location?.let {
                    currentLocation = GPSCoordinatesModel(it.latitude, it.longitude, Date(it.time))
                }
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String?) {}
            override fun onProviderDisabled(provider: String?) {}
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        //TODO: Watch for another methods of work with gps
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 0f, listener)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0f, listener)

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

    fun sendPushToken(pushToken: String?) {
        if(user !is UserModel.Authorized) return

        if(pushToken != null) {
            DeliveryServerAPI.api.sendPushToken((user as UserModel.Authorized).token, pushToken)

        }else{
            val token = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).getString("firebase_token", "notoken")
            if(token != "notoken"){
                sendPushToken(token)
                return
            }

            FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
                savePushToken(it.token)
                sendPushToken(it.token)
            }
        }
    }
}
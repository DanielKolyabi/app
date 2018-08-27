package ru.relabs.kurjer

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Created by ProOrange on 11.08.2018.
 */
class MyFirebaseMessagingService: FirebaseMessagingService() {
    override fun onNewToken(token: String?) {
        super.onNewToken(token)
        Log.d("FIREBASE", token)
    }

    override fun onMessageReceived(msg: RemoteMessage?) {
        super.onMessageReceived(msg)
        Log.d("FIREBASE", msg?.data?.map {
            it.key + ": " + it.value
        }?.joinToString("; "))
    }
}
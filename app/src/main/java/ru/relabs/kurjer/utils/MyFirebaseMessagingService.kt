package ru.relabs.kurjer.utils

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.joda.time.DateTime
import ru.relabs.kurjer.MyApplication
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.network.DeliveryServerAPI

/**
 * Created by ProOrange on 11.08.2018.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(pushToken: String) {
        super.onNewToken(pushToken)
        launch {
            (application as? MyApplication)?.let {
                it.savePushToken(pushToken)
                it.sendDeviceInfo(pushToken)
            }
        }
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        launch(UI) {
            processMessageData(msg.data)
        }
    }

    fun processMessageData(data: Map<String, String>) {
        if (data.containsKey("request_gps")) {
            (application as? MyApplication)?.user as? UserModel.Authorized ?: return
            launch {
                val coordinates = MyApplication.instance.currentLocation
                val token = (MyApplication.instance.user as UserModel.Authorized).token
                try {
                    DeliveryServerAPI.api.sendGPS(token, coordinates.lat, coordinates.long, DateTime(coordinates.time).toString("yyyy-MM-dd'T'HH:mm:ss")).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        if (data.containsKey("tasks_update")) {
            val int = Intent().apply {
                putExtra("tasks_changed", true)
                action = "NOW"
            }
            sendBroadcast(int)
        }
        if (data.containsKey("closed_task_id")) {
            run {
                val taskItemId = data["closed_task_id"]?.toIntOrNull()
                taskItemId ?: return@run

                val int = Intent().apply {
                    putExtra("task_item_closed", taskItemId)
                    action = "NOW"
                }
                sendBroadcast(int)
            }
        }
    }

}
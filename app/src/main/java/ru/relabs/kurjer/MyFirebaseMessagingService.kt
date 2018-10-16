package ru.relabs.kurjer

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.network.DeliveryServerAPI
import java.text.SimpleDateFormat

/**
 * Created by ProOrange on 11.08.2018.
 */
class MyFirebaseMessagingService: FirebaseMessagingService() {
    override fun onNewToken(pushToken: String) {
        super.onNewToken(pushToken)
        launch{
            (application as? MyApplication)?.let{
                it.savePushToken(pushToken)
                it.sendPushToken(pushToken)
            }
        }
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        launch(UI) {
            processMessageData(msg.data)
        }
    }

    fun processMessageData(data: Map<String, String>){
        if(data.containsKey("request_gps")){
           (application as? MyApplication)?.user as? UserModel.Authorized ?: return
            launch{
                val coordinates = (application as MyApplication).currentLocation
                val token = ((application as MyApplication).user as UserModel.Authorized).token
                try {
                    DeliveryServerAPI.api.sendGPS(token, coordinates.lat, coordinates.long, SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(coordinates.time)).await()
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
        if(data.containsKey("tasks_update")){
            val int = Intent().apply{
                putExtra("tasks_changed", true)
                action = "NOW"
            }
            sendBroadcast(int)
        }
        if(data.containsKey("closed_task_id")){
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
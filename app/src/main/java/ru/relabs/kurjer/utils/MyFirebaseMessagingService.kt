package ru.relabs.kurjer.utils

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import ru.relabs.kurjer.MyApplication
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.network.DeliveryServerAPI
import ru.relabs.kurjer.repository.PauseType

/**
 * Created by ProOrange on 11.08.2018.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(pushToken: String) {
        super.onNewToken(pushToken)
        GlobalScope.launch {
            (application as? MyApplication)?.let {
                it.savePushToken(pushToken)
                it.sendDeviceInfo(pushToken)
            }
        }
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        GlobalScope.launch(Dispatchers.Default) {
            processMessageData(msg.data)
        }
    }

    suspend fun processMessageData(data: Map<String, String>) {
        if (data.containsKey("request_gps")) {
            (application as? MyApplication)?.user as? UserModel.Authorized ?: return
            GlobalScope.launch {
                val coordinates = MyApplication.instance.currentLocation
                val token = (MyApplication.instance.user as UserModel.Authorized).token
                try {
                    DeliveryServerAPI.api.sendGPS(token, coordinates.lat, coordinates.long, DateTime(coordinates.time).toString("yyyy-MM-dd'T'HH:mm:ss"))
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
        if(data.containsKey("pause_start")){
            withContext(Dispatchers.Default) {
                val startTime = data["start_time"]?.toLongOrNull() ?: return@withContext
                val pauseTypeInt = data["pause_type"]?.toIntOrNull() ?: return@withContext
                val userId = data["user_id"]?.toIntOrNull() ?: return@withContext
                if (userId.toString() != (MyApplication.instance.user as? UserModel.Authorized)?.login) {
                    return@withContext
                }

                val pauseType = when (pauseTypeInt) {
                    0 -> PauseType.Lunch
                    1 -> PauseType.Load
                    else -> return@withContext
                }

                MyApplication.instance.pauseRepository.putPauseStartTime(pauseType, startTime)
                MyApplication.instance.pauseRepository.updatePauseState(pauseType)
            }
        }
        if(data.containsKey("pause_stop")){
            run{
                val stopTime = data["stop_time"]?.toLongOrNull() ?: return
                val pauseTypeInt = data["pause_type"]?.toIntOrNull() ?: return
                val userId = data["user_id"]?.toIntOrNull() ?: return
                if (userId.toString() != (MyApplication.instance.user as? UserModel.Authorized)?.login) {
                    return@run
                }

                val pauseType = when (pauseTypeInt) {
                    0 -> PauseType.Lunch
                    1 -> PauseType.Load
                    else -> return@run
                }

                MyApplication.instance.pauseRepository.putPauseEndTime(pauseType, stopTime)
                MyApplication.instance.pauseRepository.updatePauseState(pauseType)
            }
        }
    }

}
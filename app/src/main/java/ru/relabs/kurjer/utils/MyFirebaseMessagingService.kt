package ru.relabs.kurjer.utils

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.relabs.kurjer.DeliveryApp
import ru.relabs.kurjer.domain.controllers.TaskEvent
import ru.relabs.kurjer.domain.controllers.TaskEventController
import ru.relabs.kurjer.domain.models.TaskItemId
import ru.relabs.kurjer.domain.providers.FirebaseToken
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.domain.repositories.DeliveryRepository
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.domain.repositories.PauseType

/**
 * Created by ProOrange on 11.08.2018.
 */
class MyFirebaseMessagingService : FirebaseMessagingService(), KoinComponent {
    private val deliveryRepository: DeliveryRepository by inject()
    private val pauseRepository: PauseRepository by inject()
    private val scope = CoroutineScope(Dispatchers.Main)
    private val taskEventsController: TaskEventController by inject()
    private val databaseRepository: DatabaseRepository by inject()

    override fun onNewToken(pushToken: String) {
        super.onNewToken(pushToken)

        scope.launch {
            deliveryRepository.updatePushToken(FirebaseToken(pushToken))
        }
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        GlobalScope.launch(Dispatchers.Default) {
            processMessageData(msg.data)
        }
    }

    suspend fun processMessageData(data: Map<String, String>) {
        if (data.containsKey("request_gps")) {
            GlobalScope.launch {
                val coordinates = (DeliveryApp.appContext as DeliveryApp).currentLocation
//                deliveryRepository.updateLocation(coordinates)
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
            data["closed_task_id"]
                ?.toIntOrNull()
                ?.let { TaskItemId(it) }
                ?.let { taskItemId ->
                    taskEventsController.send(TaskEvent.TaskItemClosed(taskItemId))
                    databaseRepository.closeTaskItem(taskItemId, true)
                }
        }
        if (data.containsKey("pause_start")) {
            withContext(Dispatchers.Default) {
                val startTime = data["start_time"]?.toLongOrNull() ?: return@withContext
                val pauseTypeInt = data["pause_type"]?.toIntOrNull() ?: return@withContext
                val userId = data["user_id"]?.toIntOrNull() ?: return@withContext
                if (userId.toString() != (DeliveryApp.appContext as DeliveryApp).user?.login?.login) {
                    return@withContext
                }

                val pauseType = when (pauseTypeInt) {
                    0 -> PauseType.Lunch
                    1 -> PauseType.Load
                    else -> return@withContext
                }

                pauseRepository.putPauseStartTime(pauseType, startTime)
                pauseRepository.updatePauseState(pauseType)
            }
        }
        if (data.containsKey("pause_stop")) {
            run {
                val stopTime = data["stop_time"]?.toLongOrNull() ?: return
                val pauseTypeInt = data["pause_type"]?.toIntOrNull() ?: return
                val userId = data["user_id"]?.toIntOrNull() ?: return
                if (userId.toString() != (DeliveryApp.appContext as DeliveryApp).user?.login?.login) {
                    return@run
                }

                val pauseType = when (pauseTypeInt) {
                    0 -> PauseType.Lunch
                    1 -> PauseType.Load
                    else -> return@run
                }

                pauseRepository.putPauseEndTime(pauseType, stopTime)
                pauseRepository.updatePauseState(pauseType)
            }
        }
    }

}
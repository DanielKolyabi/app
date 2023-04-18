package ru.relabs.kurjer.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.relabs.kurjer.domain.controllers.TaskEvent
import ru.relabs.kurjer.domain.controllers.TaskEventController
import ru.relabs.kurjer.domain.models.TaskItemId
import ru.relabs.kurjer.domain.providers.FirebaseToken
import ru.relabs.kurjer.domain.providers.LocationProvider
import ru.relabs.kurjer.domain.repositories.TaskRepository
import ru.relabs.kurjer.domain.repositories.DeliveryRepository
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.domain.repositories.PauseType
import ru.relabs.kurjer.domain.storage.CurrentUserStorage
import ru.relabs.kurjer.utils.CustomLog

/**
 * Created by ProOrange on 11.08.2018.
 */
class MyFirebaseMessagingService : FirebaseMessagingService(), KoinComponent {
    private val deliveryRepository: DeliveryRepository by inject()
    private val pauseRepository: PauseRepository by inject()
    private val scope = CoroutineScope(Dispatchers.Main)
    private val taskEventsController: TaskEventController by inject()
    private val taskRepository: TaskRepository by inject()
    private val locationProvider: LocationProvider by inject()
    private val currentUserStorage: CurrentUserStorage by inject()

    override fun onNewToken(pushToken: String) {
        super.onNewToken(pushToken)

        scope.launch {
            deliveryRepository.updatePushToken(FirebaseToken(pushToken))
        }
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        scope.launch(Dispatchers.IO) {
            processMessageData(msg.data)
        }
    }

    suspend fun processMessageData(data: Map<String, String>) {
        if (data.containsKey("request_gps")) {
            scope.launch {
                val coordinates = locationProvider.lastReceivedLocation()
                    ?: locationProvider.updatesChannel().let {
                        val c = it.receive()
                        it.cancel()
                        c
                    }

                deliveryRepository.updateLocation(coordinates)
            }
        }
        if (data.containsKey("tasks_update")) {
            CustomLog.writeToFile("UPDATE: Got firebase tasks_update message")
            taskEventsController.send(TaskEvent.TasksUpdateRequired(false))
        }
        if (data.containsKey("closed_task_id")) {
            data["closed_task_id"]
                ?.toIntOrNull()
                ?.let { TaskItemId(it) }
                ?.let { taskItemId ->
                    taskEventsController.send(TaskEvent.TaskItemClosed(taskItemId))
                    taskRepository.closeTaskItem(taskItemId, true)
                }
        }
        if (data.containsKey("pause_start")) {
            withContext(Dispatchers.Default) {
                val startTime = data["start_time"]?.toLongOrNull() ?: return@withContext
                val pauseTypeInt = data["pause_type"]?.toIntOrNull() ?: return@withContext
                val userId = data["user_id"]?.toIntOrNull() ?: return@withContext
                if (userId.toString() != currentUserStorage.getCurrentUserLogin()?.login) {
                    return@withContext
                }

                val pauseType = when (pauseTypeInt) {
                    0 -> PauseType.Lunch
                    1 -> PauseType.Load
                    else -> return@withContext
                }

                pauseRepository.startPause(pauseType, startTime, false)
            }
        }
        if (data.containsKey("pause_stop")) {
            run {
                val stopTime = data["stop_time"]?.toLongOrNull() ?: return
                val pauseTypeInt = data["pause_type"]?.toIntOrNull() ?: return
                val userId = data["user_id"]?.toIntOrNull() ?: return
                if (userId.toString() != currentUserStorage.getCurrentUserLogin()?.login) {
                    return@run
                }

                val pauseType = when (pauseTypeInt) {
                    0 -> PauseType.Lunch
                    1 -> PauseType.Load
                    else -> return@run
                }

                pauseRepository.stopPause(pauseType, false)
            }
        }
    }

}
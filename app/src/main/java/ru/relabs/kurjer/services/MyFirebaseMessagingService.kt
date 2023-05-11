package ru.relabs.kurjer.services

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.relabs.kurjer.domain.controllers.TaskEvent
import ru.relabs.kurjer.domain.controllers.TaskEventController
import ru.relabs.kurjer.domain.models.StorageClosure
import ru.relabs.kurjer.domain.models.StorageId
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.TaskItemId
import ru.relabs.kurjer.domain.providers.FirebaseToken
import ru.relabs.kurjer.domain.providers.LocationProvider
import ru.relabs.kurjer.domain.repositories.DeliveryRepository
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.domain.repositories.PauseType
import ru.relabs.kurjer.domain.repositories.StorageRepository
import ru.relabs.kurjer.domain.repositories.TaskRepository
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
            processMessageDataLegacy(msg.data)

            val type = msg.data["type"]
            type?.let {
                processMessageData(it, msg.data)
            }
        }
    }

    private suspend fun processMessageData(type: String, data: Map<String, String>) {
        when (type) {
            "storage_close" -> {
                val taskId = data["task_id"]?.toIntOrNull() ?: return
                val storageId = data["storage_id"]?.toIntOrNull() ?: return
                val closeTime = data["close_time"]?.let { DateTime.parse(it).toDate() } ?: return
                Log.d("zxc", taskId.toString())
                val newClosure = StorageClosure(TaskId(taskId), StorageId(storageId), closeTime)
                val task = taskRepository.getTask(TaskId(taskId))
                if (task != null) {
                    val newStorage = task.storage.copy(closes = task.storage.closes + newClosure)
                    taskRepository.updateTask(task.copy(storage = newStorage))
                    taskEventsController.send(TaskEvent.TaskStorageClosed(TaskId(taskId), StorageId(storageId), closeTime))
                }
            }

            else -> FirebaseCrashlytics.getInstance().recordException(RuntimeException("Unknown type = $type"))
        }
    }

    @Deprecated("Shouldn't be used for any new push, because of \"type\" field")
    private suspend fun processMessageDataLegacy(data: Map<String, String>) {
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
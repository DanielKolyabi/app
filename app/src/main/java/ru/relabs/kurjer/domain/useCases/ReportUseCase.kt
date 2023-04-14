package ru.relabs.kurjer.domain.useCases

import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.data.database.entities.ReportQueryItemEntity
import ru.relabs.kurjer.data.database.entities.ReportQueryItemEntranceData
import ru.relabs.kurjer.domain.controllers.TaskEvent
import ru.relabs.kurjer.domain.controllers.TaskEventController
import ru.relabs.kurjer.domain.mappers.ReportEntranceSelectionMapper
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.domain.repositories.TaskRepository
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.domain.repositories.SettingsRepository
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.domain.models.GPSCoordinatesModel
import ru.relabs.kurjer.domain.repositories.QueryRepository
import ru.relabs.kurjer.utils.CustomLog
import ru.relabs.kurjer.utils.calculateDistance
import java.util.*
import kotlin.math.roundToInt

class ReportUseCase(
    private val taskRepository: TaskRepository,
    private val queryRepository: QueryRepository,
    private val tokenStorage: AuthTokenStorage,
    private val settingsRepository: SettingsRepository,
    private val taskEventController: TaskEventController
) {

    suspend fun createReport(
        task: Task,
        taskItem: TaskItem,
        location: Location?,
        batteryLevel: Float,
        isCloseTaskRequired: Boolean,
        isRejected: Boolean,
        rejectReason: String
    ) {
        val result = taskRepository.getTaskItemResult(taskItem)
        val distance = location?.let {
            calculateDistance(
                location.latitude,
                location.longitude,
                taskItem.address.lat.toDouble(),
                taskItem.address.long.toDouble()
            )
        } ?: Int.MAX_VALUE.toDouble()

        val reportItem = ReportQueryItemEntity(
            0,
            taskItem.id.id,
            task.id.id,
            taskItem.address.id.id,
            getReportLocation(location),
            Date(),
            result?.description ?: "",
            when (taskItem) {
                is TaskItem.Common -> taskItem.entrancesData.map {
                    val en = it.number
                    val resultData = result?.entrances?.firstOrNull { it.entranceNumber == en }
                    ReportQueryItemEntranceData(
                        en.number,
                        (resultData?.selection?.let { ReportEntranceSelectionMapper.toBits(it) } ?: 0),
                        resultData?.userDescription ?: "",
                        resultData?.isPhotoRequired ?: it.photoRequired
                    )
                }
                is TaskItem.Firm -> emptyList()
            },
            tokenStorage.getToken() ?: "",
            (batteryLevel * 100).roundToInt(),
            isCloseTaskRequired,
            distance.toInt(),
            taskItem.closeRadius,
            settingsRepository.isCloseRadiusRequired,
            isRejected,
            rejectReason,
            when (taskItem) {
                is TaskItem.Common -> 1
                is TaskItem.Firm -> 2
            },
            isPhotoRequired = result?.isPhotoRequired ?: taskItem.needPhoto
        )

        queryRepository.createTaskItemReport(reportItem)

        if (isCloseTaskRequired) {
            taskRepository.closeTaskItem(taskItem.id)
            taskEventController.send(TaskEvent.TaskItemClosed(taskItem.id))
            if (taskRepository.isTaskCloseRequired(taskItem.taskId)) {
                taskRepository.closeTaskById(taskItem.taskId.id)
                taskEventController.send(TaskEvent.TaskClosed(taskItem.taskId))
            }
        }
    }

    private fun getReportLocation(location: Location?) = when (location) {
        null -> GPSCoordinatesModel(0.0, 0.0, Date(0))
        else -> GPSCoordinatesModel(location.latitude, location.longitude, Date(location.time))
    }

    suspend fun isOpenedTasksExists() = taskRepository.isOpenedTasksExists()

    suspend fun isMergeNeeded(newTasks: List<Task>): Boolean = withContext(Dispatchers.IO) {
        val savedTasksIDs = taskRepository.getTaskEntityIds()

        if (newTasks.any { it.id.id !in savedTasksIDs }) {
            CustomLog.writeToFile("UPDATE (Merge): ${newTasks.firstOrNull { it.id.id !in savedTasksIDs }?.id?.id} is new")
            return@withContext true
        }

        newTasks.filter { it.id.id in savedTasksIDs }.forEach { task ->
            val savedTask = taskRepository.getTaskEntityById(task.id.id)!!
            if (task.state.state == TaskState.CANCELED) {
                CustomLog.writeToFile("UPDATE (Merge): ${task.id.id} remote task is closed")
                return@withContext true
            } else if (task.state.state == TaskState.COMPLETED) {
                CustomLog.writeToFile("UPDATE (Merge): ${task.id.id} remote task is completed")
                return@withContext true
            } else if (
                (savedTask.iteration < task.iteration)
                || (task.state.state.toInt() != savedTask.state && savedTask.state != TaskState.STARTED.toInt())
                || (task.endTime != savedTask.endTime || task.startTime != savedTask.startTime && savedTask.state != TaskState.STARTED.toInt())
            ) {
                CustomLog.writeToFile("UPDATE (Merge): ${task.id.id} time/iteration/state updated but task not started")
                CustomLog.writeToFile("UPDATE (Merge): ${task.id.id} iter: ${savedTask.iteration < task.iteration} savedIter: ${savedTask.iteration}; newTaskIter: ${task.iteration}")
                CustomLog.writeToFile("UPDATE (Merge): ${task.id.id} state: ${savedTask.state != task.state.state.toInt()} savedState: ${savedTask.state}; newTaskState: ${task.state.state.toInt()}")
                CustomLog.writeToFile("UPDATE (Merge): ${task.id.id} time: ${task.endTime != savedTask.endTime || task.startTime != savedTask.startTime} savedTime: start: ${savedTask.startTime.time}; end: ${savedTask.endTime.time}; newTaskTime: start: ${task.startTime.time} end: ${task.endTime.time}")
                return@withContext true
            }
        }
        return@withContext false
    }
}
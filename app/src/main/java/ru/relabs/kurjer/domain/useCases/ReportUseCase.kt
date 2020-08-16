package ru.relabs.kurjer.domain.useCases

import android.location.Location
import ru.relabs.kurjer.data.database.entities.ReportQueryItemEntity
import ru.relabs.kurjer.domain.controllers.TaskEvent
import ru.relabs.kurjer.domain.controllers.TaskEventController
import ru.relabs.kurjer.domain.mappers.ReportEntranceSelectionMapper
import ru.relabs.kurjer.domain.models.AllowedCloseRadius
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.domain.repositories.RadiusRepository
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.models.GPSCoordinatesModel
import ru.relabs.kurjer.utils.calculateDistance
import java.util.*
import kotlin.math.roundToInt

class ReportUseCase(
    private val databaseRepository: DatabaseRepository,
    private val pauseRepository: PauseRepository,
    private val tokenStorage: AuthTokenStorage,
    private val radiusRepository: RadiusRepository,
    private val taskEventController: TaskEventController
) {

    suspend fun createReport(task: Task, taskItem: TaskItem, location: Location?, batteryLevel: Float, isCloseTaskRequired: Boolean) {
        val result = databaseRepository.getTaskItemResult(taskItem)
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
            result?.entrances?.map {
                it.entranceNumber.number to ReportEntranceSelectionMapper.toBits(it.selection)
            } ?: emptyList(),
            tokenStorage.getToken() ?: "",
            (batteryLevel * 100).roundToInt(),
            isCloseTaskRequired,
            distance.toInt(),
            (radiusRepository.allowedCloseRadius as? AllowedCloseRadius.Required)?.distance ?: 0,
            radiusRepository.allowedCloseRadius is AllowedCloseRadius.Required
        )

        databaseRepository.createTaskItemReport(reportItem)

        if (isCloseTaskRequired) {
            databaseRepository.closeTaskItem(taskItem)
            taskEventController.send(TaskEvent.TaskItemClosed(taskItem.id))
            if (databaseRepository.isTaskCloseRequired(taskItem.taskId)) {
                databaseRepository.closeTaskById(taskItem.taskId, true)
                taskEventController.send(TaskEvent.TaskClosed(taskItem.taskId))
            }
        }
    }

    private fun getReportLocation(location: Location?) = when (location) {
        null -> GPSCoordinatesModel(0.0, 0.0, Date(0))
        else -> GPSCoordinatesModel(location.latitude, location.longitude, Date(location.time))
    }
}
package ru.relabs.kurjer.domain.mappers.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.data.database.entities.TaskItemResultEntity
import ru.relabs.kurjer.domain.mappers.TaskItemEntranceResultMapper
import ru.relabs.kurjer.domain.models.TaskItemId
import ru.relabs.kurjer.domain.models.TaskItemResult
import ru.relabs.kurjer.domain.models.TaskItemResultId

object TaskItemResultMapper {
    suspend fun fromEntity(db: AppDatabase, entity: TaskItemResultEntity): TaskItemResult = withContext(Dispatchers.IO) {
        TaskItemResult(
            id = TaskItemResultId(entity.id),
            taskItemId = TaskItemId(entity.taskItemId),
            closeTime = entity.closeTime,
            description = entity.description,
            entrances = db.entrancesDao().getByTaskItemResultId(entity.id).map {
                TaskItemEntranceResultMapper.fromEntity(it)
            },
            gps = entity.gps,
            isPhotoRequired = entity.isPhotoRequired
        )
    }

    fun fromModel(updatedReport: TaskItemResult): TaskItemResultEntity = TaskItemResultEntity(
        id = updatedReport.id.id,
        taskItemId = updatedReport.taskItemId.id,
        gps = updatedReport.gps,
        closeTime = updatedReport.closeTime,
        description = updatedReport.description,
        isPhotoRequired = updatedReport.isPhotoRequired
    )
}

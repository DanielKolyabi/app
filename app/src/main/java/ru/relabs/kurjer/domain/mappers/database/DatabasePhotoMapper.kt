package ru.relabs.kurjer.domain.mappers.database

import ru.relabs.kurjer.data.database.entities.TaskItemPhotoEntity
import ru.relabs.kurjer.domain.models.EntranceNumber
import ru.relabs.kurjer.domain.models.PhotoId
import ru.relabs.kurjer.domain.models.TaskItemId
import ru.relabs.kurjer.domain.models.TaskItemPhoto

object DatabasePhotoMapper {
    fun fromEntity(entity: TaskItemPhotoEntity): TaskItemPhoto = TaskItemPhoto(
        id = PhotoId(entity.id),
        UUID = entity.UUID,
        taskItemId = TaskItemId(entity.taskItemId),
        entranceNumber = EntranceNumber(entity.entranceNumber)
    )
}

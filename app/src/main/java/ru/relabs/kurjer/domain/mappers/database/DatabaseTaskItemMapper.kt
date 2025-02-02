package ru.relabs.kurjer.domain.mappers.database

import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.data.database.entities.TaskItemEntity
import ru.relabs.kurjer.domain.mappers.MappingException
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.TaskItemId
import ru.relabs.kurjer.domain.models.toInt
import ru.relabs.kurjer.domain.models.toTaskItemState

object DatabaseTaskItemMapper {
    suspend fun fromEntity(taskItem: TaskItemEntity, db: AppDatabase): TaskItem = when (taskItem.isFirm) {
        true -> TaskItem.Firm(
            id = TaskItemId(taskItem.id),
            address = when (val a = db.addressDao().getById(taskItem.addressId)) {
                null -> throw MappingException("address", "null")
                else -> DatabaseAddressMapper.fromEntity(a)
            },
            state = taskItem.state.toTaskItemState(),
            notes = taskItem.notes,
            subarea = taskItem.subarea,
            bypass = taskItem.bypass,
            copies = taskItem.copies,
            taskId = TaskId(taskItem.taskId),
            needPhoto = taskItem.needPhoto,
            firmName = taskItem.firmName,
            office = taskItem.officeName,
            closeRadius = taskItem.closeRadius,
            displayName = taskItem.displayName
        )

        false -> TaskItem.Common(
            id = TaskItemId(taskItem.id),
            address = when (val a = db.addressDao().getById(taskItem.addressId)) {
                null -> throw MappingException("address", "null")
                else -> DatabaseAddressMapper.fromEntity(a)
            },
            state = taskItem.state.toTaskItemState(),
            notes = taskItem.notes,
            subarea = taskItem.subarea,
            bypass = taskItem.bypass,
            copies = taskItem.copies,
            taskId = TaskId(taskItem.taskId),
            needPhoto = taskItem.needPhoto,
            entrancesData = db.entranceDataDao().getAllForTaskItem(taskItem.id).map { entity ->
                DatabaseEntranceDataMapper.fromEntity(entity)
            },
            closeRadius = taskItem.closeRadius,
            closeTime = taskItem.closeTime,
            displayName = taskItem.displayName
        )
    }

    fun toEntity(taskItem: TaskItem): TaskItemEntity = when (taskItem) {
        is TaskItem.Common -> TaskItemEntity(
            id = taskItem.id.id,
            addressId = taskItem.address.id.id,
            state = taskItem.state.toInt(),
            notes = taskItem.notes,
            subarea = taskItem.subarea,
            bypass = taskItem.bypass,
            copies = taskItem.copies,
            taskId = taskItem.taskId.id,
            needPhoto = taskItem.needPhoto,
            entrances = emptyList(), //TODO: Remove with migration
            isFirm = false,
            firmName = "",
            officeName = "",
            closeRadius = taskItem.closeRadius,
            closeTime = taskItem.closeTime,
            displayName = taskItem.displayName
        )

        is TaskItem.Firm -> TaskItemEntity(
            id = taskItem.id.id,
            addressId = taskItem.address.id.id,
            state = taskItem.state.toInt(),
            notes = taskItem.notes,
            subarea = taskItem.subarea,
            bypass = taskItem.bypass,
            copies = taskItem.copies,
            taskId = taskItem.taskId.id,
            needPhoto = taskItem.needPhoto,
            entrances = emptyList(), //TODO: Remove with migration
            isFirm = true,
            firmName = taskItem.firmName,
            officeName = taskItem.office,
            closeRadius = taskItem.closeRadius,
            closeTime = null,
            displayName = taskItem.displayName

        )
    }
}
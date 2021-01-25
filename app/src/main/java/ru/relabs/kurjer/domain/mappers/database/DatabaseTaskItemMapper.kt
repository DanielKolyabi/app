package ru.relabs.kurjer.domain.mappers.database

import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.data.database.entities.TaskItemEntity
import ru.relabs.kurjer.domain.mappers.MappingException
import ru.relabs.kurjer.domain.models.*

object DatabaseTaskItemMapper {
    fun fromEntity(taskItem: TaskItemEntity, db: AppDatabase): TaskItem = when (taskItem.isFirm) {
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
            office = taskItem.officeName
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
            entrancesData = db.entranceDataDao().getAllForTaskItem(taskItem.id).map {
                DatabaseEntranceDataMapper.fromEntity(it)
            }
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
            officeName = ""
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
            officeName = taskItem.office
        )
    }


}

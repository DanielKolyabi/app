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
            closeRadius = taskItem.closeRadius
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
                val problemApartments = parseNotes(taskItem.notes).firstOrNull { it.first == entity.number }?.second
                DatabaseEntranceDataMapper.fromEntity(entity, problemApartments)
            },
            closeRadius = taskItem.closeRadius,
            closeTime = taskItem.closeTime
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
            closeTime = taskItem.closeTime
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
            closeTime = null
        )
    }

    private fun parseNotes(notes: List<String>): List<EntranceWithApartments> {
        val fullHtml = notes.joinToString("\n").replace("<br/>", "\n")
        val strings: List<String> = fullHtml.split("\n").filter { it.startsWith("<b><font color=\"blue\">п.") }
        return strings.mapNotNull {
            val entranceNumber = it.substringAfter("<b><font color=\"blue\">п.")
                .substringBefore("</font>").toIntOrNull() ?: return@mapNotNull null
            val apartmentsNumbers = it.substringAfter("</b>")
                .split("  ")
                .filter { s -> s.startsWith("<font color=\"red\">") }
                .mapNotNull { s -> s.substringAfter("<font color=\"red\">").substringBefore("*</font>").toIntOrNull() }
            entranceNumber to apartmentsNumbers
        }
    }
}

typealias EntranceWithApartments = Pair<Int, List<Int>>
package ru.relabs.kurjer.domain.mappers.network

import ru.relabs.kurjer.data.models.tasks.TaskItemResponse
import ru.relabs.kurjer.domain.mappers.MappingException
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.TaskItemId
import ru.relabs.kurjer.domain.models.TaskItemState

object TaskItemMapper {
    fun fromRaw(raw: TaskItemResponse): TaskItem = TaskItem(
        id = TaskItemId(raw.id),
        address = AddressMapper.fromRaw(raw.address),
        state = when (raw.state) {
            0 -> TaskItemState.CREATED
            1 -> TaskItemState.CLOSED
            else -> throw MappingException("state", raw.state)
        },
        notes = raw.notes,
        entrances = raw.entrances,
        subarea = raw.subarea,
        bypass = raw.bypass,
        copies = raw.copies,
        taskId = TaskId(raw.taskId),
        needPhoto = raw.needPhoto,
        entrancesData = raw.entrancesData.map {
            EntranceMapper.fromRaw(it)
        }
    )
}
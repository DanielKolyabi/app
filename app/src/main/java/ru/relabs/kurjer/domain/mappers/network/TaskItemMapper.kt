package ru.relabs.kurjer.domain.mappers.network

import ru.relabs.kurjer.data.models.tasks.TaskItemResponse
import ru.relabs.kurjer.domain.mappers.MappingException
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.TaskItemId
import ru.relabs.kurjer.domain.models.TaskItemState

object TaskItemMapper {
    fun fromRaw(raw: TaskItemResponse): TaskItem = when (raw.isFirm) {
        true -> fromRawFirm(raw)
        false -> fromRawAddress(raw)
    }

    private fun fromRawFirm(raw: TaskItemResponse): TaskItem.Firm = TaskItem.Firm(
        id = TaskItemId(raw.id),
        address = AddressMapper.fromRaw(raw.address),
        state = when (raw.state) {
            0 -> TaskItemState.CREATED
            1 -> TaskItemState.CLOSED
            else -> throw MappingException("state", raw.state)
        },
        notes = raw.notes,
        subarea = raw.subarea,
        bypass = raw.bypass,
        copies = raw.copies,
        taskId = TaskId(raw.taskId),
        needPhoto = raw.needPhoto,
        office = raw.officeName,
        firmName = raw.firmName,
        closeRadius = raw.closeRadius,
        displayName = raw.displayName
    )

    private fun fromRawAddress(raw: TaskItemResponse): TaskItem.Common = TaskItem.Common(
        id = TaskItemId(raw.id),
        address = AddressMapper.fromRaw(raw.address),
        state = when (raw.state) {
            0 -> TaskItemState.CREATED
            1 -> TaskItemState.CLOSED
            else -> throw MappingException("state", raw.state)
        },
        notes = raw.notes,
        subarea = raw.subarea,
        bypass = raw.bypass,
        copies = raw.copies,
        taskId = TaskId(raw.taskId),
        needPhoto = raw.needPhoto,
        entrancesData = raw.entrancesData.map {
            EntranceMapper.fromRaw(it)
        },
        closeRadius = raw.closeRadius,
        closeTime = raw.closeTime,
        displayName = raw.displayName
    )
}
package ru.relabs.kurjer.domain.mappers

import ru.relabs.kurjer.data.models.tasks.TaskItemEntranceResponse
import ru.relabs.kurjer.domain.models.EntranceNumber
import ru.relabs.kurjer.domain.models.TaskItemEntrance

object EntranceMapper {
    fun fromRaw(raw: TaskItemEntranceResponse): TaskItemEntrance = TaskItemEntrance(
        number = EntranceNumber(raw.number),
        apartmentsCount = raw.apartmentsCount,
        isEuroBoxes = raw.isEuroBoxes,
        hasLookout = raw.hasLookout,
        isStacked = raw.isStacked,
        isRefused = raw.isRefused,
        photoRequired = raw.photoRequired
    )
}
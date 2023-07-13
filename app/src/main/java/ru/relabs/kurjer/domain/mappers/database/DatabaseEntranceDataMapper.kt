package ru.relabs.kurjer.domain.mappers.database

import ru.relabs.kurjer.data.database.entities.EntranceDataEntity
import ru.relabs.kurjer.domain.models.EntranceNumber
import ru.relabs.kurjer.domain.models.TaskItemEntrance
import ru.relabs.kurjer.domain.models.TaskItemId

object DatabaseEntranceDataMapper {
    fun fromEntity(entranceDataEntity: EntranceDataEntity): TaskItemEntrance = TaskItemEntrance(
        number = EntranceNumber(entranceDataEntity.number),
        apartmentsCount = entranceDataEntity.apartmentsCount,
        isEuroBoxes = entranceDataEntity.isEuroBoxes,
        hasLookout = entranceDataEntity.hasLookout,
        isStacked = entranceDataEntity.isStacked,
        isRefused = entranceDataEntity.isRefused,
        problemApartments = entranceDataEntity.problemApartments,
        photoRequired = entranceDataEntity.photoRequired
    )

    fun toEntity(it: TaskItemEntrance, id: TaskItemId) = EntranceDataEntity(
        id = 0,
        taskItemId = id.id,
        number = it.number.number,
        apartmentsCount = it.apartmentsCount,
        isEuroBoxes = it.isEuroBoxes,
        hasLookout = it.hasLookout,
        isStacked = it.isStacked,
        isRefused = it.isRefused,
        photoRequired = it.photoRequired,
        problemApartments = it.problemApartments
    )
}

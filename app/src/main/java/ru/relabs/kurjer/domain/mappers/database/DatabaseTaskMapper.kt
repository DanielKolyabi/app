package ru.relabs.kurjer.domain.mappers.database

import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.data.database.entities.TaskEntity
import ru.relabs.kurjer.domain.mappers.MappingException
import ru.relabs.kurjer.domain.models.*

object DatabaseTaskMapper {
    fun fromEntity(taskEntity: TaskEntity, db: AppDatabase): Task = Task(
        id = TaskId(taskEntity.id),
        state = Task.State(
            taskEntity.state.toTaskState(),
            taskEntity.byOtherUser
        ),
        name = taskEntity.name,
        edition = taskEntity.edition,
        copies = taskEntity.copies,
        packs = taskEntity.packs,
        remain = taskEntity.remain,
        area = taskEntity.area,
        startTime = taskEntity.startTime,
        endTime = taskEntity.endTime,
        brigade = taskEntity.brigade,
        brigadier = taskEntity.brigadier,
        rastMapUrl = taskEntity.rastMapUrl,
        userId = taskEntity.userId,
        city = taskEntity.city,
        storageAddress = taskEntity.storageAddress,
        iteration = taskEntity.iteration,
        items = db.taskItemDao().getAllForTask(taskEntity.id).map {
            DatabaseTaskItemMapper.fromEntity(it, db)
        },
        coupleType = CoupleType(taskEntity.coupleType),
        deliveryType = when (taskEntity.deliveryType) {
            1 -> TaskDeliveryType.Address
            2 -> TaskDeliveryType.Firm
            else -> throw MappingException("deliveryType", taskEntity.deliveryType)
        }
    )

    fun toEntity(task: Task): TaskEntity = TaskEntity(
        id = task.id.id,
        name = task.name,
        edition = task.edition,
        copies = task.copies,
        packs = task.packs,
        remain = task.remain,
        area = task.area,
        state = task.state.state.toInt(),
        startTime = task.startTime,
        endTime = task.endTime,
        brigade = task.brigade,
        brigadier = task.brigadier,
        rastMapUrl = task.rastMapUrl,
        userId = task.userId,
        city = task.city,
        storageAddress = task.storageAddress ?: "",
        iteration = task.iteration,
        coupleType = task.coupleType.type,
        byOtherUser = task.state.byOtherUser,
        deliveryType = when (task.deliveryType) {
            TaskDeliveryType.Address -> 1
            TaskDeliveryType.Firm -> 2
        }
    )
}
package ru.relabs.kurjer.domain.mappers.database

import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.data.database.entities.StorageClosesEntity
import ru.relabs.kurjer.data.database.entities.StorageEntity
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
        iteration = taskEntity.iteration,
        items = db.taskItemDao().getAllForTask(taskEntity.id).map {
            DatabaseTaskItemMapper.fromEntity(it, db)
        },
        coupleType = CoupleType(taskEntity.coupleType),
        deliveryType = when (taskEntity.deliveryType) {
            1 -> TaskDeliveryType.Address
            2 -> TaskDeliveryType.Firm
            else -> throw MappingException("deliveryType", taskEntity.deliveryType)
        },
        listSort = taskEntity.listSort,
        districtType = DistrictType.values()
            .getOrNull(taskEntity.districtType)
            ?: throw MappingException("districtType", taskEntity.districtType),
        orderNumber = taskEntity.orderNumber,
        editionPhotoUrl = taskEntity.editionPhotoUrl,
        storage = Task.Storage(
            address = taskEntity.storage.address,
            lat = taskEntity.storage.lat,
            long = taskEntity.storage.long,
            closeDistance = taskEntity.storage.closeDistance,
            closes = StorageCloses(
                taskId = taskEntity.storage.closes.taskId,
                storageId = taskEntity.storage.closes.storageId,
                closeDate = taskEntity.storage.closes.closeDate
            ),
            photoRequired = taskEntity.storage.photoRequired,
            requirementsUpdateDate = taskEntity.storage.requirementsUpdateDate,
            description = taskEntity.storage.description,
        ),
        storageCloseFirstRequired = taskEntity.storageCloseFirstRequired
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
        iteration = task.iteration,
        coupleType = task.coupleType.type,
        byOtherUser = task.state.byOtherUser,
        deliveryType = when (task.deliveryType) {
            TaskDeliveryType.Address -> 1
            TaskDeliveryType.Firm -> 2
        },
        listSort = task.listSort,
        districtType = task.districtType.ordinal,
        orderNumber = task.orderNumber,
        editionPhotoUrl = task.editionPhotoUrl,
        storage = StorageEntity(
            address = task.storage.address,
            lat = task.storage.lat,
            long = task.storage.long,
            closeDistance = task.storage.closeDistance,
            closes = StorageClosesEntity(
                taskId = task.storage.closes.taskId,
                storageId = task.storage.closes.storageId,
                closeDate = task.storage.closes.closeDate,
            ),
            photoRequired = task.storage.photoRequired,
            requirementsUpdateDate = task.storage.requirementsUpdateDate,
            description = task.storage.description
        ),
        storageCloseFirstRequired = task.storageCloseFirstRequired
    )

    //TODO:Сделать отдельные мапперы
}
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
        storage = StorageMapper.fromEntity(taskEntity.storage),
        storageCloseFirstRequired = taskEntity.storageCloseFirstRequired,
        displayName = taskEntity.displayName
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
        storage = StorageMapper.toEntity(task.storage),
        storageCloseFirstRequired = task.storageCloseFirstRequired,
        displayName = task.displayName
    )

    object StorageMapper {
        fun fromEntity(storageEntity: StorageEntity): Task.Storage =
            Task.Storage(
                id = StorageId(storageEntity.id),
                address = storageEntity.address,
                lat = storageEntity.latitude,
                long = storageEntity.longitude,
                closeDistance = storageEntity.closeDistance,
                closes = storageEntity.closes.map { StorageClosesMapper.fromEntity(it) },
                photoRequired = storageEntity.photoRequired,
                requirementsUpdateDate = storageEntity.requirementsUpdateDate,
                description = storageEntity.description
            )

        fun toEntity(storage: Task.Storage): StorageEntity =
            StorageEntity(
                id = storage.id.id,
                address = storage.address,
                latitude = storage.lat,
                longitude = storage.long,
                closeDistance = storage.closeDistance,
                closes = storage.closes.map { StorageClosesMapper.toEntity(it) },
                photoRequired = storage.photoRequired,
                requirementsUpdateDate = storage.requirementsUpdateDate,
                description = storage.description
            )
    }

    object StorageClosesMapper {
        fun fromEntity(closesEntity: StorageClosesEntity): StorageClosure =
            StorageClosure(
                taskId = TaskId(closesEntity.taskId),
                storageId = StorageId(closesEntity.storageId),
                closeDate = closesEntity.closeDate
            )

        fun toEntity(closes: StorageClosure): StorageClosesEntity = StorageClosesEntity(
            taskId = closes.taskId.id,
            storageId = closes.storageId.id,
            closeDate = closes.closeDate
        )
    }
}
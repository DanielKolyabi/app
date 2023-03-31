package ru.relabs.kurjer.domain.mappers.network

import ru.relabs.kurjer.data.models.tasks.TaskResponse
import ru.relabs.kurjer.domain.mappers.MappingException
import ru.relabs.kurjer.domain.models.*

object TaskMapper {
    fun fromRaw(raw: TaskResponse, deviceId: DeviceId): Task = Task(
        id = TaskId(raw.id),
        state = Task.State(
            state = when (raw.state) {
                0, 10, 11, 20 -> TaskState.CREATED
                30 -> TaskState.EXAMINED
                40, 41, 42 -> TaskState.STARTED
                50, 51, 60, 61 -> TaskState.COMPLETED
                12 -> TaskState.CANCELED
                else -> TaskState.COMPLETED
            },
            byOtherUser = raw.firstExaminedDeviceId != deviceId.id
        ),
        name = raw.name,
        edition = raw.edition,
        copies = raw.copies,
        packs = raw.packs,
        remain = raw.remain,
        area = raw.area,
        startTime = raw.startTime,
        endTime = raw.endTime,
        brigade = raw.brigade,
        brigadier = raw.brigadier,
        rastMapUrl = raw.rastMapUrl,
        userId = raw.userId,
        city = raw.city,
        iteration = raw.iteration,
        items = raw.items.map {
            TaskItemMapper.fromRaw(it)
        },
        coupleType = CoupleType(raw.coupleType),
        deliveryType = when (raw.deliveryType) {
            1 -> TaskDeliveryType.Address
            2 -> TaskDeliveryType.Firm
            else -> throw MappingException("deliveryType", raw.deliveryType)
        },
        listSort = raw.sort,
        districtType = DistrictType.values()
            .getOrNull(raw.districtType)
            ?: throw MappingException("districtType", raw.districtType),
        orderNumber = raw.orderNumber,
        editionPhotoUrl = raw.photos.firstOrNull(),
        storage = Task.Storage(
            id = StorageId(raw.storageId),
            address = raw.storageAddress,
            lat = raw.storageLat,
            long = raw.storageLong,
            closeDistance = raw.storageCloseDistance,
            closes = raw.storageCloses.map {
                StorageClosure(
                    taskId = TaskId(it.taskId),
                    storageId = StorageId(it.storageId),
                    closeDate = it.closeDate
                )
            },
            photoRequired = raw.storagePhotoRequired,
            requirementsUpdateDate = raw.storageRequirementsUpdateDate,
            description = raw.storageDescription
        ),
        storageCloseFirstRequired = raw.storageCloseFirstRequired,
        displayName = raw.displayName
    )
}
package ru.relabs.kurjer.domain.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import ru.relabs.kurjer.domain.mappers.MappingException
import java.util.*

@Parcelize
data class TaskId(val id: Int) : Parcelable

@Parcelize
data class CoupleType(val type: Int) : Parcelable

@Parcelize
data class StorageId(val id: Int) : Parcelable

enum class DistrictType {
    Global,
    Regional,
}

@Parcelize
data class Task(
    val id: TaskId,
    val state: State,
    val name: String,
    val edition: Int,
    val copies: Int,
    val packs: Int,
    val remain: Int,
    val area: Int,
    val startTime: Date,
    val endTime: Date,
    val brigade: Int,
    val brigadier: String,
    val rastMapUrl: String,
    val userId: Int,
    val city: String,
    val iteration: Int,
    val items: List<TaskItem>,
    val coupleType: CoupleType,
    val deliveryType: TaskDeliveryType,
    val listSort: String,
    val districtType: DistrictType,
    val orderNumber: Int,
    val editionPhotoUrl: String?,
    val storage: Storage,
    val storageCloseFirstRequired: Boolean
) : Parcelable {

    val listName: String
        get() = "${name} №${edition}, ${copies}экз., (${brigade}бр/${area}уч)"

    val storageListName: String
        get() = "${name} №${edition}, (${brigade}бр/${area}уч), ${copies}экз., ${packs}пач., ${remain}шт."

    @Parcelize
    data class State(
        val state: TaskState,
        val byOtherUser: Boolean
    ) : Parcelable

    @Parcelize
    data class Storage(
        val id: StorageId,
        val address: String,
        val lat: Float,
        val long: Float,
        val closeDistance: Int,
        val closes: List<StorageClosure>,
        val photoRequired: Boolean,
        val requirementsUpdateDate: Date,
        val description: String
    ) : Parcelable
}

@Parcelize
data class StorageClosure(
    val taskId: TaskId,
    val storageId: StorageId,
    val closeDate: Date
) : Parcelable

fun Task.canBeSelectedWith(tasks: List<Task>): Boolean {
    return when (districtType) {
        DistrictType.Global -> true
        DistrictType.Regional -> tasks.all {
            it.districtType == DistrictType.Global || (it.districtType == DistrictType.Regional && it.orderNumber == orderNumber)
        }
    }
}

enum class TaskDeliveryType {
    Address, Firm
}


enum class TaskState {
    CREATED,
    EXAMINED,
    STARTED,
    COMPLETED,
    CANCELED;

    fun toInt(): Int {
        return when (this) {
            CREATED -> 1
            EXAMINED -> 2
            STARTED -> 3
            COMPLETED -> 4
            CANCELED -> 5
        }
    }
}

fun Int.toTaskState() = when (this) {
    1 -> TaskState.CREATED
    2 -> TaskState.EXAMINED
    3 -> TaskState.STARTED
    4 -> TaskState.COMPLETED
    5 -> TaskState.CANCELED
    else -> throw MappingException("state", this)
}
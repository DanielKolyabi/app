package ru.relabs.kurjer.domain.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import ru.relabs.kurjer.data.database.entities.TaskItemEntity
import ru.relabs.kurjer.domain.mappers.MappingException

@Parcelize
data class TaskItemId(val id: Int): Parcelable

@Parcelize
data class TaskItem(
    val id: TaskItemId,
    val address: Address,
    val state: TaskItemState,
    val notes: List<String>,
    val subarea: Int,
    val bypass: Int,
    val copies: Int,
    val taskId: TaskId,
    val needPhoto: Boolean,
    val entrancesData: List<TaskItemEntrance>
): Parcelable

enum class TaskItemState{
    CREATED, CLOSED
}

fun TaskItemState.toInt() = when(this){
    TaskItemState.CREATED -> TaskItemEntity.STATE_CREATED
    TaskItemState.CLOSED -> TaskItemEntity.STATE_CLOSED
}

fun Int.toTaskItemState() = when(this){
    TaskItemEntity.STATE_CREATED -> TaskItemState.CREATED
    TaskItemEntity.STATE_CLOSED -> TaskItemState.CLOSED
    else -> throw MappingException("state", this)
}
package ru.relabs.kurjer.domain.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import ru.relabs.kurjer.domain.mappers.MappingException

@Parcelize
data class TaskItemId(val id: Int): Parcelable

@Parcelize
data class TaskItem(
    val id: TaskItemId,
    val address: Address,
    val state: TaskItemState,
    val notes: List<String>,
    val entrances: List<Int>,
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
    TaskItemState.CREATED -> 0
    TaskItemState.CLOSED -> 1
}

fun Int.toTaskItemState() = when(this){
    0 -> TaskItemState.CREATED
    1 -> TaskItemState.CLOSED
    else -> throw MappingException("state", this)
}
package ru.relabs.kurjer.domain.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

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
    val taskId: Int,
    val needPhoto: Boolean,
    val entrancesData: List<TaskItemEntrance>
): Parcelable

enum class TaskItemState{
    CREATED, CLOSED
}
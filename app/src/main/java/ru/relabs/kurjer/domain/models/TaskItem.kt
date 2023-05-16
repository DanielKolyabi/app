package ru.relabs.kurjer.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

import ru.relabs.kurjer.data.database.entities.TaskItemEntity
import ru.relabs.kurjer.domain.mappers.MappingException
import java.util.Date

@Parcelize
data class TaskItemId(val id: Int) : Parcelable

sealed class TaskItem : Parcelable {
    abstract val closeRadius: Int

    @Parcelize
    data class Common(
        val id: TaskItemId,
        val address: Address,
        val state: TaskItemState,
        val notes: List<String>,
        val subarea: Int,
        val bypass: Int,
        val copies: Int,
        val taskId: TaskId,
        val needPhoto: Boolean,
        val entrancesData: List<TaskItemEntrance>,
        override val closeRadius: Int,
        val closeTime: Date?
    ) : TaskItem()

    @Parcelize
    data class Firm(
        val id: TaskItemId,
        val address: Address,
        val state: TaskItemState,
        val notes: List<String>,
        val subarea: Int,
        val bypass: Int,
        val copies: Int,
        val taskId: TaskId,
        val needPhoto: Boolean,
        val office: String,
        val firmName: String,
        override val closeRadius: Int
    ) : TaskItem()
}

val TaskItem.id
    get() = when (this) {
        is TaskItem.Common -> id
        is TaskItem.Firm -> id
    }

val TaskItem.address
    get() = when (this) {
        is TaskItem.Common -> address
        is TaskItem.Firm -> address
    }

val TaskItem.state
    get() = when (this) {
        is TaskItem.Common -> state
        is TaskItem.Firm -> state
    }

val TaskItem.notes
    get() = when (this) {
        is TaskItem.Common -> notes
        is TaskItem.Firm -> notes
    }

val TaskItem.subarea
    get() = when (this) {
        is TaskItem.Common -> subarea
        is TaskItem.Firm -> subarea
    }

val TaskItem.bypass
    get() = when (this) {
        is TaskItem.Common -> bypass
        is TaskItem.Firm -> bypass
    }

val TaskItem.copies
    get() = when (this) {
        is TaskItem.Common -> copies
        is TaskItem.Firm -> copies
    }

val TaskItem.taskId
    get() = when (this) {
        is TaskItem.Common -> taskId
        is TaskItem.Firm -> taskId
    }

val TaskItem.needPhoto
    get() = when (this) {
        is TaskItem.Common -> needPhoto
        is TaskItem.Firm -> needPhoto
    }


enum class TaskItemState {
    CREATED, CLOSED
}

fun TaskItemState.toInt() = when (this) {
    TaskItemState.CREATED -> TaskItemEntity.STATE_CREATED
    TaskItemState.CLOSED -> TaskItemEntity.STATE_CLOSED
}

fun Int.toTaskItemState() = when (this) {
    TaskItemEntity.STATE_CREATED -> TaskItemState.CREATED
    TaskItemEntity.STATE_CLOSED -> TaskItemState.CLOSED
    else -> throw MappingException("state", this)
}
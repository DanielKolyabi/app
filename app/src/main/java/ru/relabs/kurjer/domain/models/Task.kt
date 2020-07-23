package ru.relabs.kurjer.domain.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class TaskId(val id: Int) : Parcelable

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
    val storageAddress: String?,
    val iteration: Int,
    val items: List<TaskItem>,
    val coupleType: Int
) : Parcelable {

    @Parcelize
    data class State(
        val state: TaskState,
        val byOtherUser: Boolean
    ) : Parcelable
}


enum class TaskState {
    CREATED,
    EXAMINED,
    STARTED,
    COMPLETED,
    CANCELED;

    @Deprecated("Should be removed after refactoring")
    fun toInt(): Int {
        return when(this){
            CREATED -> 0
            EXAMINED -> 1
            STARTED -> 2
            COMPLETED -> 4
            CANCELED -> 16
        }
    }
}
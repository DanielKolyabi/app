package ru.relabs.kurjer.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import ru.relabs.kurjer.data.database.entities.TaskEntity
import java.util.*

@Parcelize
data class TaskModel(
        var id: Int,
        var name: String,
        var edition: Int,
        var copies: Int,
        var packs: Int,
        var remain: Int,
        var area: Int,
        var state: Int,
        var startTime: Date,
        var endTime: Date,
        var brigade: Int,
        var brigadier: String,
        var rastMapUrl: String,
        var userId: Int,
        var items: List<TaskItemModel>,
        var city: String,
        var storageAddress: String,
        var iteration: Int,
        //Temporary var, for some features in lists
        var selected: Boolean,
        var coupleType: Int
) : Parcelable {
    val plainState
        get() = if(state and TaskModel.BY_OTHER_USER == 1){
            state xor TaskModel.BY_OTHER_USER
        }else{
            state
        }

    val displayName
        get() = "${name} №${edition}, ${copies}экз., (${brigade}бр/${area}уч)"

    fun toTaskEntity(): TaskEntity {
        return TaskEntity(
                id, name, edition, copies, packs, remain, area, state, startTime, endTime, brigade, brigadier, rastMapUrl, userId,
                city, storageAddress, iteration, coupleType
        )
    }

    fun isAvailableByDate(date: Date): Boolean = (date >= startTime)
    fun canShowedByDate(date: Date): Boolean = date <= Date(endTime.time)

    companion object {
        val CREATED = 0
        val EXAMINED = 1
        val STARTED = 2
        val COMPLETED = 4
        val CANCELED = 16
        val BY_OTHER_USER = 8
        val TASK_STATE_MASK = 7
    }
}
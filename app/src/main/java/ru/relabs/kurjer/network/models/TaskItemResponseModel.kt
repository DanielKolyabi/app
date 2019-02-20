package ru.relabs.kurjer.network.models

import com.google.gson.annotations.SerializedName
import ru.relabs.kurjer.models.EntranceModel
import ru.relabs.kurjer.models.TaskItemModel

data class TaskItemResponseModel(
        var address: AddressResponseModel,
        var state: Int,
        @SerializedName("note")
        var notes: List<String>,
        var id: Int,
        var entrances: List<Int>,
        var subarea: Int,
        var bypass: Int,
        var copies: Int,
        @SerializedName("task_id")
        var taskId: Int,
        @SerializedName("need_photo")
        var needPhoto: Boolean
) {
    fun toTaskItemModel(): TaskItemModel {
        return TaskItemModel(address.toAddressModel(), state, id, notes, entrances.map{ EntranceModel(it, false) }, subarea, bypass, copies, needPhoto)
    }
}

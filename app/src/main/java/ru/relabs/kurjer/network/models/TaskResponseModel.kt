package ru.relabs.kurjer.network.models

import com.google.gson.annotations.SerializedName
import ru.relabs.kurjer.models.TaskModel
import java.util.*

/**
 * Created by ProOrange on 05.09.2018.
 */

data class TaskResponseModel(
        val id: Int,
        val name: String,
        val edition: Int,
        val copies: Int,
        val packs: Int,
        val remain: Int,
        val area: Int,
        val state: Int,
        @SerializedName("start_time")
        val startTime: Date,
        @SerializedName("end_time")
        val endTime: Date,
        val region: Int,
        val brigade: Int,
        val brigadier: String,
        @SerializedName("rast_map_url")
        val rastMapUrl: String,
        @SerializedName("user_id")
        val userId: Int,
        val city: String,
        @SerializedName("storage_address")
        val storageAddress: String,
        val iteration: Int,
        val items: List<TaskItemResponseModel>
) {
    fun toTaskModel(): TaskModel {
        return TaskModel(
                id, name, edition, copies, packs, remain, area, state, startTime, endTime, region, brigade, brigadier, rastMapUrl, userId,
                items.map{it.toTaskItemModel()}, city, storageAddress, iteration, false

        )
    }
}
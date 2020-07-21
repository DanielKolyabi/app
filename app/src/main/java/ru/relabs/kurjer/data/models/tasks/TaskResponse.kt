package ru.relabs.kurjer.data.models.tasks

import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * Created by ProOrange on 05.09.2018.
 */

data class TaskResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("edition") val edition: Int,
    @SerializedName("copies") val copies: Int,
    @SerializedName("packs") val packs: Int,
    @SerializedName("remain") val remain: Int,
    @SerializedName("area") val area: Int,
    @SerializedName("state") val state: Int,
    @SerializedName("start_time") val startTime: Date,
    @SerializedName("end_time") val endTime: Date,
    @SerializedName("brigade") val brigade: Int,
    @SerializedName("brigadier") val brigadier: String,
    @SerializedName("rast_map_url") val rastMapUrl: String,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("city") val city: String,
    @SerializedName("storage_address") val storageAddress: String?,
    @SerializedName("iteration") val iteration: Int,
    @SerializedName("items") val items: List<TaskItemResponse>,
    @SerializedName("first_examined_device_id") val firstExaminedDeviceId: String?,
    @SerializedName("couple_type") val coupleType: Int
)
//{
//    fun toTaskModel(deviceId: String): TaskModel {
//        return TaskModel(
//                id, name, edition, copies, packs, remain, area, fromSiriusState(deviceId), startTime, endTime, brigade, brigadier, rastMapUrl, userId,
//                items.map{it.toTaskItemModel()}, city, storageAddress ?: "", iteration, false, coupleType
//
//        )
//    }
//
//    private fun fromSiriusState(deviceId: String): Int {
//        val newState = when (state) {
//            0, 10, 11, 20 -> TaskModel.CREATED
//            30 -> TaskModel.EXAMINED
//            40, 41, 42 -> TaskModel.STARTED
//            50, 51, 60, 61 -> TaskModel.COMPLETED
//            12 -> TaskModel.CANCELED
//            else -> TaskModel.COMPLETED
//        }
//
//        if(newState == TaskModel.CANCELED){
//            return newState
//        }
//
//        if(newState > TaskModel.CREATED && deviceId != firstExaminedDeviceId){
//            return newState xor TaskModel.BY_OTHER_USER
//        }
//
//        return newState
//    }
//}
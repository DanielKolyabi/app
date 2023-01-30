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
    @SerializedName("storage_address") val storageAddress: String,
    @SerializedName("storage_lat") val storageLat: Float,
    @SerializedName("storage_long") val storageLong: Float,
    @SerializedName("iteration") val iteration: Int,
    @SerializedName("items") val items: List<TaskItemResponse>,
    @SerializedName("first_examined_device_id") val firstExaminedDeviceId: String?,
    @SerializedName("couple_type") val coupleType: Int,
    @SerializedName("delivery_type") val deliveryType: Int,
    @SerializedName("sort") val sort: String,
    @SerializedName("district_type") val districtType: Int,
    @SerializedName("order_number") val orderNumber: Int,
    @SerializedName("photo_paths") val photos: List<String>,
    @SerializedName("storage_close_distance") val storageCloseDistance: Int,
    @SerializedName("storage_closes") val storageCloses: StorageClosesResponse,
    @SerializedName("storage_photo_required") val storagePhotoRequired: Boolean,
    @SerializedName("storage_close_first_required") val storageCloseFirstRequired: Boolean,
    @SerializedName("storage_requirements_update_date") val storageRequirementsUpdateDate: Date,
    @SerializedName("storage_description") val storageDescription : String
    )
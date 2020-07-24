package ru.relabs.kurjer.data.models.tasks

import com.google.gson.annotations.SerializedName
import ru.relabs.kurjer.data.models.auth.AddressResponse
import ru.relabs.kurjer.models.EntranceDataModel

data class TaskItemResponse(
    @SerializedName("address") val address: AddressResponse,
    @SerializedName("state") val state: Int,
    @SerializedName("note") val notes: List<String>,
    @SerializedName("id") val id: Int,
    @SerializedName("entrances") val entrances: List<Int>,
    @SerializedName("subarea") val subarea: Int,
    @SerializedName("bypass") val bypass: Int,
    @SerializedName("copies") val copies: Int,
    @SerializedName("task_id") val taskId: Int,
    @SerializedName("need_photo") val needPhoto: Boolean,
    @SerializedName("entrances_data") val entrancesData: List<TaskItemEntranceResponse>
)
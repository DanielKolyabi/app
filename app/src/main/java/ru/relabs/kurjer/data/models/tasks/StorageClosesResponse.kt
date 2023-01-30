package ru.relabs.kurjer.data.models.tasks

import com.google.gson.annotations.SerializedName
import java.util.Date

data class StorageClosesResponse(
    @SerializedName("task_id")
    val taskId: Int,
    @SerializedName("storage_id")
    val storageId: Int,
    @SerializedName("close_date")
    val closeDate: Date
)

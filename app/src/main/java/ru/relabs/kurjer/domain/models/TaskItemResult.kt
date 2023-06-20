package ru.relabs.kurjer.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class TaskItemResultId(val id: Int) : Parcelable

@Parcelize
data class TaskItemResult(
    val id: TaskItemResultId,
    val taskItemId: TaskItemId,
    val closeTime: Date?,
    val description: String,
    val entrances: List<TaskItemEntranceResult>,
    val gps: GPSCoordinatesModel,
    val isPhotoRequired: Boolean
) : Parcelable
package ru.relabs.kurjer.models

import java.util.*

/**
 * Created by ProOrange on 03.09.2018.
 */

data class TaskItemResultModel(
        val id: Int,
        val taskItem: TaskItemModel,
        val gps: GPSCoordinatesModel,
        val closeTime: Date?,
        val userDescription: String,
        val entrances: List<TaskItemResultEntranceModel>
)
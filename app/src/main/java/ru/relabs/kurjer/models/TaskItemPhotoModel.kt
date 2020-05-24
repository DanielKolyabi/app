package ru.relabs.kurjer.models

import android.net.Uri
import ru.relabs.kurjer.files.PathHelper
import java.util.*

/**
 * Created by ProOrange on 03.09.2018.
 */
data class TaskItemPhotoModel(
        val id: Int,
        val uuid: String,
        val taskItem: TaskItemModel,
        val gps: GPSCoordinatesModel,
        val entranceNumber: Int
) {

    fun getPhotoURI(): Uri = Uri.fromFile(PathHelper.getTaskItemPhotoFile(taskItem, UUID.fromString(uuid)))
}
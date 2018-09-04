package ru.relabs.kurjer.files

import android.os.Environment
import ru.relabs.kurjer.models.TaskItemModel
import java.io.File
import java.net.URI
import java.util.*

/**
 * Created by ProOrange on 03.09.2018.
 */

object PathHelper {
    val dataPath = Environment.getExternalStorageDirectory().path + File.separator + "deliveryman" + File.separator
    val photoPath = dataPath + "photos" + File.separator

    fun getTaskItemPhotoFolder(taskItem: TaskItemModel): File {
        val taskDir = File(photoPath+File.separator+taskItem.id)
        if(!taskDir.exists()) taskDir.mkdirs()
        return taskDir
    }

    fun getTaskItemPhotoFile(taskItem: TaskItemModel, uuid: UUID): File {
        val picture = File(getTaskItemPhotoFolder(taskItem), uuid.toString()+".jpg")
        return picture
    }
}
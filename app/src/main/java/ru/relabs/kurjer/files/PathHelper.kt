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

    fun getTaskItemPhotoFolderById(taskItemID: Int): File {
        val taskDir = File(photoPath+File.separator+taskItemID)
        if(!taskDir.exists()) taskDir.mkdirs()
        return taskDir
    }

    fun getTaskItemPhotoFile(taskItem: TaskItemModel, uuid: UUID): File {
        return getTaskItemPhotoFileByID(taskItem.id, uuid)
    }

    fun getTaskItemPhotoFileByID(taskItemID: Int, uuid: UUID): File {
        val picture = File(getTaskItemPhotoFolderById(taskItemID), uuid.toString()+".jpg")
        return picture
    }
}
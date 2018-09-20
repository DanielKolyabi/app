package ru.relabs.kurjer.files

import android.os.Environment
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import java.io.File
import java.util.*

/**
 * Created by ProOrange on 03.09.2018.
 */

object PathHelper {
    val dataPath = Environment.getExternalStorageDirectory().path + File.separator + "deliveryman" + File.separator
    val photoPath = dataPath + "photos" + File.separator
    val mapPath = dataPath + "maps" + File.separator
    init {
        val mapDir = File(mapPath+File.separator)
        if(!mapDir.exists()) mapDir.mkdirs()
    }

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

    fun getTaskRasterizeMapFile(task: TaskModel): File {
        return getTaskRasterizeMapFileById(task.id)
    }

    fun getTaskRasterizeMapFileById(taskId: Int): File {
        val mapDir = File(mapPath)
        if(!mapDir.exists()) mapDir.mkdirs()
        return File(mapDir, taskId.toString()+".jpg")
    }

    fun getUpdateFile(): File {
        return File(dataPath, "update.apk")
    }
}
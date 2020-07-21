package ru.relabs.kurjer.files

import android.os.Environment
import ru.relabs.kurjer.DeliveryApp
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import java.io.File
import java.util.*

/**
 * Created by ProOrange on 03.09.2018.
 */

object PathHelper {
    private val dataRootDir = DeliveryApp.appContext.applicationContext.filesDir
    private val updatesPath = Environment.getExternalStorageDirectory().path + File.separator + "deliveryman" + File.separator
    private val photoDir = File(dataRootDir, "photos").apply {
        mkdirs()
    }
    private val mapDir = File(dataRootDir, "maps").apply {
        mkdirs()
    }

    fun getTaskItemPhotoFolderById(taskItemID: Int): File {
        val taskDir = File(photoDir, taskItemID.toString())
        if (!taskDir.exists()) taskDir.mkdirs()
        return taskDir
    }

    fun getTaskItemPhotoFile(taskItem: TaskItemModel, uuid: UUID): File {
        return getTaskItemPhotoFileByID(taskItem.id, uuid)
    }

    fun getTaskItemPhotoFileByID(taskItemID: Int, uuid: UUID): File {
        return File(getTaskItemPhotoFolderById(taskItemID), "$uuid.jpg")
    }

    fun getTaskRasterizeMapFile(task: TaskModel): File {
        return getTaskRasterizeMapFileById(task.id)
    }

    fun getTaskRasterizeMapFileById(taskId: Int): File {
        return File(mapDir, "$taskId.jpg")
    }

    fun getUpdateFile(): File {
        return File(updatesPath, "update.apk")
    }
}
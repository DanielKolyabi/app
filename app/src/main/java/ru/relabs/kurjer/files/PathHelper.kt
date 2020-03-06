package ru.relabs.kurjer.files

import ru.relabs.kurjer.MyApplication
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import java.io.File
import java.util.*

/**
 * Created by ProOrange on 03.09.2018.
 */

object PathHelper {
    private val dataRootDir = MyApplication.instance.applicationContext.filesDir
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
        return File(dataRootDir, "update.apk")
    }
}
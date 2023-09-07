package ru.relabs.kurjer.domain.providers

import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.id
import java.io.File
import java.util.UUID

class PathsProvider(
    private val filesRootDir: File
) {
    private val updatesPath = File(filesRootDir, UPDATES).apply {
        mkdirs()
    }
    private val photoDir = File(filesRootDir, PHOTOS).apply {
        mkdirs()
    }
    private val storagePhotoDir = File(filesRootDir, STORAGE_PHOTOS).apply {
        mkdirs()
    }
    private val mapDir = File(filesRootDir, MAPS).apply {
        mkdirs()
    }
    private val editionPhotoDir = File(filesRootDir, EDITIONS).apply {
        mkdirs()
    }

    fun getDirectories(): List<File> =
        listOf(updatesPath, photoDir, storagePhotoDir, mapDir, editionPhotoDir)

    fun getCrashLogFile(): File {
        return File(filesRootDir, "crash.log")
    }

    fun getStoragePhotoFileById(reportId: Int, uuid: UUID): File {
        return File(getStoragePhotoFolderById(reportId), "$uuid.jpg")
    }

    private fun getStoragePhotoFolderById(reportId: Int): File {
        val storageDir = File(storagePhotoDir, reportId.toString())
        if (!storageDir.exists()) storageDir.mkdirs()
        return storageDir
    }

    fun getTaskItemPhotoFolderById(taskItemID: Int): File {
        val taskDir = File(photoDir, taskItemID.toString())
        if (!taskDir.exists()) taskDir.mkdirs()
        return taskDir
    }

    fun getTaskItemPhotoFile(taskItem: TaskItem, uuid: UUID): File {
        return getTaskItemPhotoFileByID(taskItem.id.id, uuid)
    }

    fun getTaskItemPhotoFileByID(taskItemID: Int, uuid: UUID): File {
        return File(getTaskItemPhotoFolderById(taskItemID), "$uuid.jpg")
    }

    fun getTaskRasterizeMapFile(task: Task): File {
        return getTaskRasterizeMapFileById(task.id)
    }

    fun getTaskRasterizeMapFileById(taskId: TaskId): File {
        return File(mapDir, "${taskId.id}.jpg")
    }

    fun getEditionPhotoFile(task: Task): File {
        return getEditionPhotoFileById(task.id)
    }

    fun getEditionPhotoFileById(taskId: TaskId): File {
        return File(editionPhotoDir, "${taskId.id}.jpg")
    }

    fun getUpdateFile(): File {
        return File(updatesPath, "update.apk")
    }

    companion object {
        fun getDirNames() = listOf(UPDATES, PHOTOS, STORAGE_PHOTOS, MAPS, EDITIONS)
        private const val UPDATES = "updates"
        private const val PHOTOS = "photos"
        private const val STORAGE_PHOTOS = "storage_photos"
        private const val MAPS = "maps"
        private const val EDITIONS = "editions"
    }
}
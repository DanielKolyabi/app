package ru.relabs.kurjer.persistence

import android.os.Environment
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.withContext
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.application
import ru.relabs.kurjer.files.PathHelper
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.persistence.entities.ReportQueryItemEntity
import ru.relabs.kurjer.persistence.entities.SendQueryItemEntity
import java.util.*


/**
 * Created by ProOrange on 05.09.2018.
 */

object PersistenceHelper {

    fun getFreeMemorySpace(): Long {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        return bytesAvailable / (1024 * 1024)
    }

    fun removeUnusedClosedTasks(db: AppDatabase) {
        //Remove all closed tasks, that haven't any report in query
        db.taskDao().allClosed.forEach {
            if (db.reportQueryDao().getByTaskId(it.id).isEmpty()) {
                db.taskDao().delete(it)
            }
        }
    }

    fun closeTaskById(db: AppDatabase, taskId: Int) {
        //Remove all taskItems
        db.taskItemDao().getAllForTask(taskId).forEach { taskItem ->
            db.taskItemResultsDao().getByTaskItemId(taskItem.id)?.let { taskItemResult ->
                db.entrancesDao().getByTaskItemResultId(taskItemResult.id).forEach { entrance ->
                    db.entrancesDao().delete(entrance)
                }
                db.taskItemResultsDao().delete(taskItemResult)
            }
            db.entranceDataDao().deleteAllForTaskItem(taskItem.id)
            db.taskItemDao().delete(taskItem)
        }
        //Remove task
        db.taskDao().getById(taskId)?.let {
            db.taskDao().delete(it)
        }
        //Remove rast map
        PathHelper.getTaskRasterizeMapFileById(taskId).delete()
    }


    fun closeTask(db: AppDatabase, task: TaskModel) {
        closeTaskById(db, task.id)
    }

    fun removeReport(db: AppDatabase, report: ReportQueryItemEntity) {
        db.reportQueryDao().delete(report)
        db.photosDao().getByTaskItemId(report.taskItemId).forEach {
            //Delete photo
            val file = PathHelper.getTaskItemPhotoFileByID(report.taskItemId, UUID.fromString(it.UUID))
            file.delete()
            db.photosDao().delete(it)
        }
        PathHelper.getTaskItemPhotoFolderById(report.taskItemId).delete()
    }

    suspend fun isMergeNeeded(
            db: AppDatabase,
            newTasks: List<TaskModel>
    ): Boolean {
        return withContext(CommonPool) {
            val savedTasksIDs = db.taskDao().all.map { it.id }
            val newTasksIDs = newTasks.map { it.id }

            newTasks.filter { it.id !in savedTasksIDs }.forEach { task ->
                return@withContext true
            }

            newTasks.filter { it.id in savedTasksIDs }.forEach { task ->
                val savedTask = db.taskDao().getById(task.id)!!
                if (task.state == TaskModel.CANCELED) {
                    return@withContext true
                } else if (task.state == TaskModel.COMPLETED) {
                    return@withContext true
                } else if (
                        (savedTask.iteration < task.iteration)
                        || (task.toTaskEntity().state != savedTask.state && savedTask.state != TaskModel.STARTED)
                        || (task.endTime != savedTask.endTime || task.startTime != savedTask.startTime && savedTask.state != TaskModel.STARTED)
                ) {
                    return@withContext true
                }
            }
            return@withContext false
        }
    }

    suspend fun merge(
            db: AppDatabase,
            newTasks: List<TaskModel>,
            onNewTaskAppear: (task: TaskModel) -> Unit,
            onTaskChanged: (oldTask: TaskModel, newTask: TaskModel) -> Unit
    ): MergeResult {
        val result = MergeResult(false, false)
        withContext(CommonPool) {
            val savedTasksIDs = db.taskDao().all.map { it.id }
            val newTasksIDs = newTasks.map { it.id }

            //Задача отсутствует в ответе от сервера (удалено)
            db.taskDao().all.filter { it.id !in newTasksIDs }.forEach { task ->
                closeTask(db, task.toTaskModel(db))
                result.isTasksChanged = true
                Log.d("merge", "Close task: ${task.id}")
            }

            //Задача не присутствует в сохранённых (новая)
            newTasks.filter { it.id !in savedTasksIDs }.forEach { task ->
                if (task.state == TaskModel.CANCELED) {
                    Log.d("merge", "New task ${task.id} passed due 12 status")
                    return@forEach
                }
                //Add task
                val newTaskId = db.taskDao().insert(task.toTaskEntity())
                Log.d("merge", "Add task ID: $newTaskId")
                var openedTaskItems = 0
                task.items.forEach { item ->
                    //Add address
                    db.addressDao().insert(item.address.toAddressEntity())
                    //Add item
                    val reportForThisTask = db.reportQueryDao().getByTaskItemId(item.id)
                    if (reportForThisTask != null) {
                        item.state = TaskItemModel.CLOSED
                    }
                    if (item.state != TaskItemModel.CLOSED) {
                        openedTaskItems++
                    }
                    val newId = db.taskItemDao().insert(item.toTaskItemEntity(task.id))
                    db.entranceDataDao().insertAll(item.entrancesData.map { enData -> enData.toEntity(item.id) })
                    Log.d("merge", "Add taskItem ID: $newId")
                }
                if (openedTaskItems <= 0) {
                    closeTaskById(db, newTaskId.toInt())
                } else {
                    result.isNewTasksAdded = true
                    onNewTaskAppear(task)
                }
            }

            //Задача есть и на сервере и на клиенте (мерж)
            /*
            Если она закрыта | выполнена на сервере - удалить с клиента
            Если итерация > сохранённой | состояние отличается от сохранённого и сохранённое != начато |
             */
            newTasks.filter { it.id in savedTasksIDs }.forEach { task ->
                val savedTask = db.taskDao().getById(task.id) ?: return@forEach
                if (task.state == TaskModel.CANCELED) {
                    if (savedTask.plainState == TaskModel.STARTED) {
                        db.sendQueryDao().insert(
                                SendQueryItemEntity(0,
                                        BuildConfig.API_URL + "/api/v1/tasks/${savedTask.id}/accepted?token=" + (application().user as UserModel.Authorized).token,
                                        ""
                                )
                        )
                    } else {
                        closeTask(db, savedTask.toTaskModel(db))
                    }
                } else if (task.plainState == TaskModel.COMPLETED) {
                    closeTask(db, savedTask.toTaskModel(db))
                    return@forEach
                } else if (
                        (savedTask.iteration < task.iteration)
                        || (task.toTaskEntity().state != savedTask.state && savedTask.plainState != TaskModel.STARTED)
                        || (task.endTime != savedTask.endTime || task.startTime != savedTask.startTime)
                ) {

                    val examinedByOtherUser = if (savedTask.state == TaskModel.CREATED
                            && task.toTaskEntity().state == TaskModel.EXAMINED) TaskModel.BY_OTHER_USER else 0

                    onTaskChanged(savedTask.toTaskModel(db), task)

                    db.taskDao().update(task.toTaskEntity().apply {
                        state = state xor examinedByOtherUser
                    })

                    val currentTasks = db.taskItemDao().getAllForTask(task.id).toMutableList()

                    //Add new tasks and update old tasks
                    task.items.forEach { newTaskItem ->
                        currentTasks.removeAll { oldTaskItem -> oldTaskItem.id == newTaskItem.id}

                        db.addressDao().insert(newTaskItem.address.toAddressEntity())

                        val reportForThisTask = db.reportQueryDao().getByTaskItemId(newTaskItem.id)
                        if (reportForThisTask != null) {
                            newTaskItem.state = TaskItemModel.CLOSED
                        }
                        db.taskItemDao().insert(newTaskItem.toTaskItemEntity(task.id))
                        db.entranceDataDao().insertAll(newTaskItem.entrancesData.map { enData -> enData.toEntity(newTaskItem.id) })
                    }

                    //Remove old taskItems
                    currentTasks.forEach {
                        removeTaskItem(db, it.id)
                    }
                    result.isTasksChanged = true
                }
            }
        }
        return result
    }

    private fun removeTaskItem(db: AppDatabase, taskItemId: Int) {
        if(db.reportQueryDao().getByTaskItemId(taskItemId) == null){
            db.photosDao().deleteByTaskItemId(taskItemId)
        }
        val result = db.taskItemResultsDao().getByTaskItemId(taskItemId)
        if(result != null){
            db.entrancesDao().getByTaskItemResultId(result.id)
            db.taskItemResultsDao().delete(result)
        }
        db.entranceDataDao().deleteAllForTaskItem(taskItemId)
        db.taskItemDao().deleteById(taskItemId)
    }
}

data class MergeResult(
        var isTasksChanged: Boolean,
        var isNewTasksAdded: Boolean
)

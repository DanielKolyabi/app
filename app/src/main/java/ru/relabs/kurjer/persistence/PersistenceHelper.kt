package ru.relabs.kurjer.persistence

import android.util.Log
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.withContext
import ru.relabs.kurjer.files.PathHelper
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.persistence.entities.ReportQueryItemEntity
import java.io.File
import java.util.*

/**
 * Created by ProOrange on 05.09.2018.
 */

object PersistenceHelper {

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
    ): Boolean{
        return withContext(CommonPool) {
            val savedTasksIDs = db.taskDao().all.map { it.id }
            val newTasksIDs = newTasks.map { it.id }

            newTasks.filter { it.id !in savedTasksIDs }.forEach { task ->
                return@withContext true
            }

            newTasks.filter { it.id in savedTasksIDs }.forEach { task ->
                val savedTask = db.taskDao().getById(task.id)!!
                if (savedTask.state == TaskModel.STARTED || savedTask.state == TaskModel.COMPLETED) {
                    if (task.state == 60 || task.state == 50) {
                        return@withContext true
                    }
                } else {
                    if (task.iteration <= savedTask.iteration && task.state == savedTask.state) return@forEach
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

            //Process tasks that not existed in newTasks
            db.taskDao().all.filter { it.id !in newTasksIDs }.forEach { task ->
                //Close task because it not existed on server
                closeTask(db, task.toTaskModel(db))
                result.isTasksChanged = true
                Log.d("merge", "Close task: ${task.id}")
            }

            //Process not existed new tasks
            newTasks.filter { it.id !in savedTasksIDs }.forEach { task ->
                //Add task
                val newTaskId = db.taskDao().insert(task.toTaskEntity())
                result.isNewTasksAdded = true
                onNewTaskAppear(task)
                Log.d("merge", "Add task ID: $newTaskId")
                task.items.forEach { item ->
                    //Add address
                    if (db.addressDao().getById(item.address.id) == null) {
                        db.addressDao().insert(item.address.toAddressEntity())
                    }
                    //Add item
                    val reportForThisTask = db.taskItemResultsDao().getByTaskItemId(item.id)
                    if(reportForThisTask != null){
                        item.state = TaskItemModel.CLOSED
                    }
                    val newId = db.taskItemDao().insert(item.toTaskItemEntity(task.id))
                    Log.d("merge", "Add taskItem ID: $newId")
                }
            }

            //Process existed new tasks
            newTasks.filter { it.id in savedTasksIDs }.forEach { task ->
                val savedTask = db.taskDao().getById(task.id)!!
                //Task in work or completed
                if (savedTask.state == TaskModel.STARTED || savedTask.state == TaskModel.COMPLETED) {
                    //Closed or completed
                    if (task.state == 60 || task.state == 50) {
                        closeTask(db, savedTask.toTaskModel(db))
                        return@forEach
                    } else {
                        //ignore, should i return state 40 and iteration into server?
                    }
                    //Task not in work
                } else {
                    if (task.iteration <= savedTask.iteration && task.toTaskEntity().state == savedTask.state) return@forEach

                    val examinedByOtherUser = if (db.taskDao().getById(task.id)!!.state == TaskModel.CREATED
                            && task.toTaskEntity().state == TaskModel.EXAMINED) TaskModel.BY_OTHER_USER else 0

                    onTaskChanged(savedTask.toTaskModel(db), task)

                    db.taskDao().update(task.toTaskEntity().apply {
                        state = state xor examinedByOtherUser
                    })

                    task.items.forEach {
                        if (db.addressDao().getById(it.address.id) == null) {
                            db.addressDao().insert(it.address.toAddressEntity())
                        }

                        val reportForThisTask = db.taskItemResultsDao().getByTaskItemId(it.id)
                        if(reportForThisTask != null){
                            it.state = TaskItemModel.CLOSED
                        }

                        db.taskItemDao().insert(it.toTaskItemEntity(task.id))
                    }
                    result.isTasksChanged = true
                }
            }
        }
        return result
    }
}

data class MergeResult(
        var isTasksChanged: Boolean,
        var isNewTasksAdded: Boolean
)

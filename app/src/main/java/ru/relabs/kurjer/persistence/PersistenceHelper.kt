package ru.relabs.kurjer.persistence

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.withContext
import ru.relabs.kurjer.files.PathHelper
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.persistence.entities.AddressEntity
import ru.relabs.kurjer.persistence.entities.ReportQueryItemEntity
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

    fun closeTask(db: AppDatabase, task: TaskModel) {
        //Remove all taskItems
        db.taskItemDao().getAllForTask(task.id).forEach { taskItem ->
            db.taskItemResultsDao().getByTaskItemId(taskItem.id)?.let { taskItemResult ->
                db.entrancesDao().getByTaskItemResultId(taskItemResult.id).forEach { entrance ->
                    db.entrancesDao().delete(entrance)
                }
                db.taskItemResultsDao().delete(taskItemResult)
            }
            db.taskItemDao().delete(taskItem)
        }
    }

    suspend fun removeReport(db: AppDatabase, report: ReportQueryItemEntity) {
        db.reportQueryDao().delete(report)
        db.photosDao().getByTaskItemId(report.taskItemId).forEach {
            //Delete photo
            val file = PathHelper.getTaskItemPhotoFileByID(report.taskItemId, UUID.fromString(it.UUID))
            file.delete()
            db.photosDao().delete(it)
        }
    }

    suspend fun merge(db: AppDatabase, newTasks: List<TaskModel>): Boolean {
        var isSomethingChanged = false
        withContext(CommonPool) {
            val savedTasksIDs = db.taskDao().all.map { it.id }
            val newTasksIDs = db.taskDao().all.map { it.id }
            //Process not existed new tasks
            newTasks.filter { it.id !in savedTasksIDs }.forEach { task ->
                //Add task
                db.taskDao().insert(task.toTaskEntity().apply {
                    state = fromSiriusState()
                })
                task.items.forEach { item ->
                    //Add address
                    if (db.addressDao().getById(item.address.id) == null) {
                        db.addressDao().insert(AddressEntity(item.address.id, item.address.street, item.address.house))
                    }
                    //Add item
                    db.taskItemDao().insert(item.toTaskItemEntity(task.id))
                }
            }

            //Process tasks that not existed in newTasks
            db.taskDao().all.filter { it.id !in newTasksIDs }.forEach { task ->
                //Close task because it not existed on server
                closeTask(db, task.toTaskModel(db))
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
                    if (task.iteration <= savedTask.iteration) return@forEach

                    val examinedByOtherUser = if(db.taskDao().getById(task.id)!!.state == TaskModel.CREATED
                            && task.toTaskEntity().fromSiriusState() == TaskModel.EXAMINED) TaskModel.BY_OTHER_USER else 0

                    db.taskDao().update(task.toTaskEntity().apply {
                        state = fromSiriusState() and examinedByOtherUser
                    })
                    task.items.forEach {
                        if (db.addressDao().getById(it.address.id) == null) {
                            db.addressDao().insert(it.address.toAddressEntity())
                        }
                        db.taskItemDao().update(it.toTaskItemEntity(task.id))
                    }
                    isSomethingChanged = true
                    if (savedTask.state and TaskModel.EXAMINED != 0) {
                        //TODO: Notify user about changes
                    }
                }
            }
        }
        return isSomethingChanged
    }
}

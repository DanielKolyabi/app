package ru.relabs.kurjer.persistence

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.withContext
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel

/**
 * Created by ProOrange on 05.09.2018.
 */

object PersistenceHelper {
    suspend fun isMergeNeeded(db: AppDatabase, newTasks: List<TaskModel>): Boolean {
        return true
    }

    suspend fun merge(db: AppDatabase, newTasks: List<TaskModel>, stategy: MergeStrategy) {
        withContext(CommonPool) {
            //Task disappear
            val oldTasks = db.taskDao().all
            val newTasksIDs = newTasks.map { it.id }
            oldTasks.forEach {
                if (it.id !in newTasksIDs) {
                    stategy.onTaskDisappear(db, it.toTaskModel(db))
                }
            }

            newTasks.forEach { task ->
                val savedTask = db.taskDao().getById(task.id)
                //Task appear
                if (savedTask == null) {
                    stategy.onTaskAppear(db, task)
                    return@forEach
                }
                //Task changed
                val savedTaskModel = savedTask.toTaskModel(db)
                if (stategy.isTaskChanged(task, savedTaskModel)) {
                    stategy.onTaskChanged(db, task, savedTaskModel)
                }

                val savedTaskItems = db.taskItemDao().getAllForTask(savedTask.id)
                val savedTaskItemsIDs = savedTaskItems.map { it.id }
                //Appear task items
                val newTaskItems = task.items.filter { it.id !in savedTaskItemsIDs }
                newTaskItems.forEach { newTaskItem ->
                    stategy.onTaskItemAppear(db, newTaskItem)
                }
                //Disappear task items
                val newTaskItemsIDs = task.items.map { it.id }
                val lostTaskItems = savedTaskItems.filter { it.id !in newTaskItemsIDs }
                lostTaskItems.forEach { lostTaskItem ->
                    stategy.onTaskItemDisappear(db, lostTaskItem.toTaskItemModel(db))
                }
                //Changed task items
                savedTaskItemsIDs.intersect(newTaskItemsIDs).forEach { id ->
                    val newItem = task.items.find { it.id == id }!!
                    val oldItem = savedTaskItems.find { it.id == id }!!.toTaskItemModel(db)

                    if (stategy.isTaskItemChanged(newItem, oldItem)) {
                        stategy.onTaskItemChanged(db, newItem, oldItem)
                    }
                }
            }
        }
    }
}

interface MergeStrategy {
    fun onTaskItemAppear(db: AppDatabase, newTaskItem: TaskItemModel)
    fun onTaskItemDisappear(db: AppDatabase, oldTaskItem: TaskItemModel)
    fun onTaskItemChanged(db: AppDatabase, newItem: TaskItemModel, oldItem: TaskItemModel)

    fun onTaskAppear(db: AppDatabase, newTask: TaskModel)
    fun onTaskDisappear(db: AppDatabase, oldTask: TaskModel)
    fun onTaskChanged(db: AppDatabase, newTask: TaskModel, oldTask: TaskModel)

    fun isTaskChanged(newTask: TaskModel, oldTask: TaskModel): Boolean {
        return newTask.startTime != oldTask.startTime ||
                newTask.endTime != oldTask.endTime ||
                newTask.state != oldTask.state ||
                newTask.area != oldTask.area ||
                newTask.brigade != oldTask.brigade ||
                newTask.brigadier != oldTask.brigadier ||
                newTask.city != oldTask.city ||
                newTask.copies != oldTask.copies ||
                newTask.edition != oldTask.edition ||
                newTask.name != oldTask.name ||
                newTask.packs != oldTask.packs ||
                newTask.rastMapUrl != oldTask.rastMapUrl ||
                newTask.region != oldTask.region ||
                newTask.remain != oldTask.remain ||
                newTask.storageAddress != oldTask.storageAddress
    }

    fun isTaskItemChanged(newItem: TaskItemModel, oldItem: TaskItemModel): Boolean {
        return newItem.address.id != oldItem.address.id
                || newItem.state != oldItem.state
                || newItem.entrances != oldItem.entrances
                || newItem.notes != oldItem.notes
                || newItem.bypass != oldItem.bypass
                || newItem.copies != oldItem.copies
                || newItem.subarea != oldItem.subarea
    }
}

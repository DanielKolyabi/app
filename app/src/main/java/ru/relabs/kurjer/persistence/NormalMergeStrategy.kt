package ru.relabs.kurjer.persistence

import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel

/**
 * Created by ProOrange on 05.09.2018.
 */
class NormalMergeStrategy : MergeStrategy {
    override fun onTaskItemAppear(db: AppDatabase, newTaskItem: TaskItemModel) {

    }

    override fun onTaskItemChanged(db: AppDatabase, newItem: TaskItemModel, oldItem: TaskItemModel) {

    }

    override fun onTaskAppear(db: AppDatabase, newTask: TaskModel) {

    }

    override fun onTaskChanged(db: AppDatabase, newTask: TaskModel, oldTask: TaskModel) {

    }

    override fun onTaskDisappear(db: AppDatabase, oldTask: TaskModel) {

    }

    override fun onTaskItemDisappear(db: AppDatabase, oldTaskItem: TaskItemModel) {

    }
}
package ru.relabs.kurjer.ui.presenters

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.MyApplication
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.fragments.TaskListFragment
import ru.relabs.kurjer.ui.models.TaskListModel

/**
 * Created by ProOrange on 27.08.2018.
 */
class TaskListPresenter(val fragment: TaskListFragment) {
    fun onTaskSelected(pos: Int) {
        val task = (fragment.adapter.data[pos] as? TaskListModel.Task)?.task ?: return
        if (!isTaskCanSelected(task)) {
            (fragment.context as? MainActivity)?.showError("Вы должны ознакомиться с заданием.")
            return
        }

        task.apply {
            selected = !selected
        }
        fragment.adapter.notifyItemChanged(pos)
        updateStartButton()
    }

    fun onTaskClicked(pos: Int) {
        val task = (fragment.adapter.data[pos] as? TaskListModel.Task)?.task ?: return
        (fragment.context as MainActivity).showTaskDetailsScreen(task)
    }

    fun onStartClicked() {
        (fragment.activity as? MainActivity)?.showAddressListScreen(
                fragment.adapter.data.filter {
                    (it is TaskListModel.Task) && it.task.selected
                }.map {
                    (it as TaskListModel.Task).task
                }
        )
    }

    fun updateStartButton() {
        fragment.setStartButtonActive(isStartAvailable())
    }

    fun isTaskCanSelected(task: TaskModel): Boolean {
        return task.state == TaskModel.EXAMINED || task.state == TaskModel.STARTED
    }

    fun isStartAvailable(): Boolean =
            fragment.adapter.data.any {
                if (it !is TaskListModel.Task) return@any false
                it.task.selected
            }

    fun loadTasks() {
        launch(UI) {
            val db = (fragment.activity!!.application as MyApplication).database
            val tasks = withContext(CommonPool) { db.taskDao().all.map { it.toTaskModel(db) } }
            fragment.adapter.data.addAll(tasks.filter { it.state != TaskModel.COMPLETED }.map { TaskListModel.Task(it) })
            fragment.adapter.notifyDataSetChanged()
            updateStartButton()
            fragment.showListLoading(false)
        }
    }
}
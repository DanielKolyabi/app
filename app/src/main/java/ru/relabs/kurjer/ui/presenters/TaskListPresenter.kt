package ru.relabs.kurjer.ui.presenters

import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.MyApplication
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.fragments.TaskListFragment

/**
 * Created by ProOrange on 27.08.2018.
 */
class TaskListPresenter(val fragment: TaskListFragment) {
    fun onTaskSelected(pos: Int) {
        if(!isTaskCanSelected(fragment.adapter.data[pos])){
            (fragment.context as? MainActivity)?.showError("Вы должны ознакомиться с заданием.")
            return
        }

        fragment.adapter.data[pos].apply {
            selected = !selected
        }
        fragment.adapter.notifyItemChanged(pos)
        updateStartButton()
    }

    fun onTaskClicked(pos: Int) {
        (fragment.context as MainActivity).showTaskDetailsScreen(fragment.adapter.data[pos])
    }

    fun onStartClicked() {
        (fragment.activity as? MainActivity)?.showAddressListScreen(
                fragment.adapter.data.filter {
                    it.selected
                }
        )
    }

    fun updateStartButton() {
        fragment.setStartButtonActive(isStartAvailable())
    }

    fun isTaskCanSelected(task: TaskModel): Boolean {
        return task.state == 1 || task.state == 2
    }

    fun isStartAvailable(): Boolean =
            fragment.adapter.data.any {
                it.selected && it.state > 0
            }

    fun loadTasks() {
        launch(UI) {
            val db = (fragment.activity!!.application as MyApplication).database
            val tasks = db.taskDao().all
            tasks.forEach {
                it.items = db.taskItemDao().getAllForTask(it.id)
                it.items.forEach {
                    it.address = db.addressDao().getById(it.addressId)
                }
            }
            fragment.adapter.data.addAll(tasks)
            fragment.adapter.notifyDataSetChanged()
            updateStartButton()
        }
    }
}
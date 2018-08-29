package ru.relabs.kurjer.ui.presenters

import kotlinx.android.synthetic.main.fragment_task_list.*
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.adapters.TaskListAdapter
import ru.relabs.kurjer.ui.fragments.TaskListFragment

/**
 * Created by ProOrange on 27.08.2018.
 */
class TaskListPresenter(val fragment: TaskListFragment) {
    fun onTaskSelected(pos: Int) {
        fragment.adapter.data[pos].apply {
            selected = !selected
        }
        fragment.adapter.notifyItemChanged(pos)
    }

    fun onTaskClicked(pos: Int) {
        (fragment.context as MainActivity).showTaskDetailsScreen(fragment.adapter.data[pos])
    }

    fun onStartClicked() {
        (fragment.activity as? MainActivity)?.showAddressListScreen()
    }
}
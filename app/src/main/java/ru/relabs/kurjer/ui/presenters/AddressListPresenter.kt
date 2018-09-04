package ru.relabs.kurjer.ui.presenters

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.MyApplication
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.fragments.AddressListFragment
import ru.relabs.kurjer.ui.helpers.TaskAddressSorter
import ru.relabs.kurjer.ui.models.AddressListModel

/**
 * Created by ProOrange on 09.08.2018.
 */
class AddressListPresenter(val fragment: AddressListFragment) {

    var sortingMethod = TaskAddressSorter.ALPHABETIC
    val tasks = mutableListOf<TaskModel>()

    fun changeSortingMethod(sorting: Int) {
        sortingMethod = sorting
        applySorting()
    }

    fun applySorting() {
        val items = mutableListOf<AddressListModel.TaskItem>()
        checkTasksIsClosed()

        tasks.forEach { task ->
            items.addAll(
                    task.items.map {
                        AddressListModel.TaskItem(it, task)
                    }
            )
        }
        fragment.adapter.data.clear()
        if (tasks.size == 1) {
            fragment.adapter.data.add(AddressListModel.SortingItem(sortingMethod))
        }
        fragment.adapter.data.addAll(prepareTaskItemsForList(items))
        fragment.adapter.notifyDataSetChanged()
    }

    private fun checkTasksIsClosed() {
        tasks.removeAll {
            if(isAllTaskItemsClosed(it)){
                closeTask(it)
                return@removeAll true
            }
            return@removeAll false
        }

        if(tasks.size == 0){
            (fragment.context as MainActivity).showTaskListScreen()
        }
    }

    private fun closeTask(task: TaskModel) {
        launch {
            val db = (fragment.activity!!.application as MyApplication).database
            db.taskDao().update(
                    db.taskDao().getById(task.id).let{
                        it.state = TaskModel.COMPLETED
                        it
                    }
            )
        }
    }

    fun prepareTaskItemsForList(taskItems: List<AddressListModel.TaskItem>): List<AddressListModel> {
        val sorted = if (sortingMethod == TaskAddressSorter.ALPHABETIC) {
            TaskAddressSorter.sortTaskItemsAlphabetic(taskItems)
        } else {
            TaskAddressSorter.sortTaskItemsStandart(taskItems)
        }

        return TaskAddressSorter.getAddressesWithTasksList(sorted)
    }

    private fun isAllTaskItemsClosed(task: TaskModel): Boolean{
        return !task.items.any { it.state != TaskItemModel.CLOSED }
    }

    fun onItemClicked(addressId: Int, taskId: Int) {
        (fragment.context as? MainActivity)?.showTasksReportScreen(fragment.adapter.data.filter {
            (it is AddressListModel.TaskItem) && it.taskItem.address.id == addressId
        }.map {
            it as AddressListModel.TaskItem
        }, taskId)
    }

    fun updateStates() {
        launch(UI) {
            val db = (fragment.activity!!.application as MyApplication).database

            withContext(CommonPool) {
                tasks.forEach { task ->
                    task.items.map { item ->
                        val savedState = db.taskItemDao().getById(item.id).state
                        if (savedState != item.state) {
                            item.state = savedState
                        }
                    }
                }
            }

            applySorting()
        }
    }

}


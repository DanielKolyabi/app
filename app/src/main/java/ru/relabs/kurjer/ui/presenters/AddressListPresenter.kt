package ru.relabs.kurjer.ui.presenters

import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.ui.models.AddressListModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.fragments.AddressListFragment
import ru.relabs.kurjer.ui.helpers.TaskAddressSorter

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

    fun prepareTaskItemsForList(taskItems: List<AddressListModel.TaskItem>): List<AddressListModel> {
        val sorted = if (sortingMethod == TaskAddressSorter.ALPHABETIC) {
            TaskAddressSorter.sortTaskItemsAlphabetic(taskItems)
        } else {
            TaskAddressSorter.sortTaskItemsStandart(taskItems)
        }
        return TaskAddressSorter.getAddressesWithTasksList(sorted)
    }

    fun onItemClicked(addressId: Int) {
        (fragment.context as? MainActivity)?.showTasksReportScreen(fragment.adapter.data.filter {
            (it is AddressListModel.TaskItem) && it.taskItem.address.id == addressId
        }.map {
            it as AddressListModel.TaskItem
        })
    }

}


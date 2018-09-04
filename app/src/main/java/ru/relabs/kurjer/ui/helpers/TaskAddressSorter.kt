package ru.relabs.kurjer.ui.helpers

import ru.relabs.kurjer.ui.models.AddressListModel

/**
 * Created by ProOrange on 31.08.2018.
 */
object TaskAddressSorter {

    fun getAddressesWithTasksList(taskItems: List<AddressListModel.TaskItem>): List<AddressListModel> {
        val result = mutableListOf<AddressListModel>()
        var lastAddressId = -1
        taskItems.forEach {
            if (lastAddressId != it.taskItem.address.id) {
                lastAddressId = it.taskItem.address.id
                result.add(AddressListModel.Address(it.taskItem))
            }
            result.add(AddressListModel.TaskItem(it.taskItem, it.parentTask))
        }
        return result
    }

    fun sortTaskItemsStandart(taskItems: List<AddressListModel.TaskItem>): List<AddressListModel.TaskItem> {
        return taskItems.sortedWith(compareBy<AddressListModel.TaskItem> { it.taskItem.state }
                .thenBy { it.taskItem.subarea }
                .thenBy { it.taskItem.bypass }
                .thenBy { it.taskItem.address.street }
                .thenBy { it.taskItem.address.house })
    }

    fun sortTaskItemsAlphabetic(taskItems: List<AddressListModel.TaskItem>): List<AddressListModel.TaskItem> {
        return taskItems.sortedWith(compareBy<AddressListModel.TaskItem> { it.taskItem.state }
                .thenBy { it.taskItem.address.street }
                .thenBy { it.taskItem.address.house })
    }

    val STANDART = 1
    val ALPHABETIC = 2
}
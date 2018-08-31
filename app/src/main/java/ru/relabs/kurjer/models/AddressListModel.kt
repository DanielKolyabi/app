package ru.relabs.kurjer.models

/**
 * Created by ProOrange on 09.08.2018.
 */
sealed class AddressListModel {

    data class Address(
            val taskItem: TaskItemModel
    ) : AddressListModel()

    data class TaskItem(
            val taskItem: TaskItemModel,
            val parentTask: TaskModel
    ) : AddressListModel()

    data class SortingItem(val sortType: Int) : AddressListModel()
}
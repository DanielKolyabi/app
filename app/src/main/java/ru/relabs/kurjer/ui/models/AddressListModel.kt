package ru.relabs.kurjer.ui.models

import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel

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
    object Loader : AddressListModel()
}
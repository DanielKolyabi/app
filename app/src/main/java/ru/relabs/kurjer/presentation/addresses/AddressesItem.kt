package ru.relabs.kurjer.presentation.addresses

import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskItem

sealed class AddressesItem {

    data class GroupHeader(val subItems: List<TaskItem>, val showBypass: Boolean) : AddressesItem()
    data class AddressItem(val taskItem: TaskItem.Common, val task: Task) : AddressesItem()
    data class FirmItem(val taskItem: TaskItem.Firm, val task: Task) : AddressesItem()
}


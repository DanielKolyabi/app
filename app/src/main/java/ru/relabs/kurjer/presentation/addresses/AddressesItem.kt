package ru.relabs.kurjer.presentation.addresses

import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskItem

sealed class AddressesItem {

    data class GroupHeader(val task: Task, val subItems: List<TaskItem>, val showBypass: Boolean): AddressesItem()
    data class AddressItem(val taskItem: TaskItem, val task: Task): AddressesItem()
    data class Sorting(val sorting: AddressesSortingMethod): AddressesItem()

    object Loading: AddressesItem()
    object Blank: AddressesItem()
    data class Search(val filter: String): AddressesItem()
}


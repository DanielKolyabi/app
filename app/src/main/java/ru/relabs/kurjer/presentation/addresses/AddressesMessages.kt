package ru.relabs.kurjer.presentation.addresses

import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.base.tea.msgEffects
import ru.relabs.kurjer.presentation.base.tea.msgEmpty
import ru.relabs.kurjer.presentation.base.tea.msgState

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

object AddressesMessages {
    fun msgInit(taskIds: List<TaskId>): AddressesMessage = msgEffects(
        { it },
        { listOf(AddressesEffects.effectLoadTasks(taskIds)) }
    )

    fun msgAddLoaders(i: Int): AddressesMessage =
        msgState { it.copy(loaders = it.loaders + i) }

    fun msgTaskItemClicked(item: TaskItem, task: Task): AddressesMessage = msgEmpty() //Show report
    fun msgTaskItemMapClicked(task: Task): AddressesMessage = msgEmpty() //Show rasterized map
    fun msgAddressMapClicked(task: Task): AddressesMessage = msgEmpty() //Show yandex map with ability to select address

    fun msgSortingChanged(sorting: AddressesSortingMethod): AddressesMessage =
        msgState { it.copy(sorting = sorting) }

    fun msgTasksLoaded(tasks: List<Task>): AddressesMessage =
        msgState { it.copy(tasks = tasks) }

    fun msgNavigateBack(): AddressesMessage =
        msgEffect(AddressesEffects.effectNavigateBack())

    fun msgSearch(searchText: String): AddressesMessage =
        msgState { it.copy(searchFilter = searchText) }
}
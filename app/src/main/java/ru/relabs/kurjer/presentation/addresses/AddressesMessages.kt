package ru.relabs.kurjer.presentation.addresses

import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.base.tea.msgEffects
import ru.relabs.kurjer.presentation.base.tea.msgState
import ru.relabs.kurjer.utils.SearchUtils

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

object AddressesMessages {
    fun msgInit(taskIds: List<TaskId>): AddressesMessage = msgEffects(
        { it },
        {
            listOf(
                AddressesEffects.effectLoadTasks(taskIds),
                AddressesEffects.effectLaunchEventConsumer()
            )
        }
    )

    fun msgAddLoaders(i: Int): AddressesMessage =
        msgState { it.copy(loaders = it.loaders + i) }

    fun msgTaskItemClicked(item: TaskItem, task: Task): AddressesMessage =
        msgEffect(AddressesEffects.effectNavigateReport(task, item))

    fun msgTaskItemMapClicked(task: Task): AddressesMessage =
        msgEffect(AddressesEffects.effectOpenImageMap(task))

    fun msgAddressMapClicked(addressTaskItems: List<TaskItem>): AddressesMessage =
        msgEffect(AddressesEffects.effectOpenYandexMap(addressTaskItems))

    fun msgSortingChanged(sorting: AddressesSortingMethod): AddressesMessage =
        msgState { it.copy(sorting = sorting) }

    fun msgTasksLoaded(tasks: List<Task>): AddressesMessage =
        msgState { it.copy(tasks = tasks) }

    fun msgNavigateBack(): AddressesMessage = msgEffects(
        { it.copy(exits = it.exits.inc()) },
        { listOf(AddressesEffects.effectNavigateBack(exits = it.exits)) }
    )

    fun msgSearch(searchText: String): AddressesMessage =
        msgState { it.copy(searchFilter = searchText) }

    fun msgTaskItemClosed(taskItemId: TaskItemId): AddressesMessage = msgEffects(
        { s ->
            val newTasks = s.tasks.map { t ->
                t.copy(
                    items = t.items.map { ti ->
                        if (ti.id == taskItemId) {
                            when (ti) {
                                is TaskItem.Common -> ti.copy(state = TaskItemState.CLOSED)
                                is TaskItem.Firm -> ti.copy(state = TaskItemState.CLOSED)
                            }
                        } else {
                            ti
                        }
                    }
                )
            }
            val visibleOpenedTaskItemsCount = newTasks.map { t ->
                t.items.filter {
                    val isNameMatch = when (it) {
                        is TaskItem.Common -> false
                        is TaskItem.Firm -> SearchUtils.isMatches(
                            "${it.firmName}, ${it.office}",
                            s.searchFilter
                        )
                    } || SearchUtils.isMatches(it.address.name, s.searchFilter)

                    isNameMatch && it.state == TaskItemState.CREATED
                }.size
            }.sum()

            s.copy(
                tasks = newTasks,
                searchFilter = if (visibleOpenedTaskItemsCount == 0) {
                    ""
                } else {
                    s.searchFilter
                }
            )
        },
        {
            listOf(
                AddressesEffects.effectValidateTasks()
            )
        }
    )

    fun msgRemoveTask(id: TaskId): AddressesMessage = msgEffects(
        { s ->
            val newTasks = s.tasks.filter { it.id != id }
            s.copy(tasks = newTasks, exits = if (newTasks.isEmpty()) s.exits.inc() else s.exits)
        },
        { s ->
            listOfNotNull(
                AddressesEffects.effectNavigateBack(true, s.exits).takeIf { s.tasks.isEmpty() }
            )
        }
    )

    fun msgSelectedListAddress(address: Address?): AddressesMessage =
        msgState { it.copy(selectedListAddress = address) }

    fun msgGlobalMapClicked(): AddressesMessage = msgEffects(
        { it },
        { s ->
            listOf(AddressesEffects.effectOpenYandexMap(s.tasks.flatMap { t -> t.items }))
        }
    )

    fun msgStorageBtnClicked(): AddressesMessage = msgEffect(
        AddressesEffects.effectNavigateStorage()
    )
}
package ru.relabs.kurjer.presentation.addresses

import ru.relabs.kurjer.domain.models.*
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

    fun msgTaskItemClosed(taskItemId: TaskItemId): AddressesMessage =
        msgState { s -> //TODO: Check if have opened items, if no - stop timer | closeTask in database
            s.copy(
                tasks = s.tasks.map { t ->
                    t.copy(
                        items = t.items.map { ti ->
                            if (ti.id == taskItemId) {
                                ti.copy(state = TaskItemState.CLOSED)
                            } else {
                                ti
                            }
                        }
                    )
                }
            )
        }

    fun msgTaskClosed(taskId: TaskId): AddressesMessage = msgEffects(
        { s ->
            s.copy(
                tasks = s.tasks.map { t ->
                    if (t.id == taskId) {
                        t.copy(state = t.state.copy(state = TaskState.COMPLETED))
                    } else {
                        t
                    }
                }
            )
        },
        { s ->
            listOfNotNull(
                AddressesEffects.effectNavigateBack().takeIf { s.tasks.none { it.state.state == TaskState.STARTED } }
            )
        }
    )
}
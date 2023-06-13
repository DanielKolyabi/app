package ru.relabs.kurjer.presentation.addresses

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.relabs.kurjer.domain.controllers.TaskEventController
import ru.relabs.kurjer.domain.models.Address
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.TaskItemState
import ru.relabs.kurjer.domain.models.address
import ru.relabs.kurjer.domain.models.bypass
import ru.relabs.kurjer.domain.models.state
import ru.relabs.kurjer.domain.models.subarea
import ru.relabs.kurjer.domain.providers.PathsProvider
import ru.relabs.kurjer.domain.repositories.TaskRepository
import ru.relabs.kurjer.presentation.base.tea.ElmEffect
import ru.relabs.kurjer.presentation.base.tea.ElmMessage
import ru.relabs.kurjer.presentation.base.tea.ElmRender
import ru.relabs.kurjer.presentation.base.tea.ErrorContext
import ru.relabs.kurjer.presentation.base.tea.ErrorContextImpl
import ru.relabs.kurjer.presentation.base.tea.RouterContext
import ru.relabs.kurjer.presentation.base.tea.RouterContextMainImpl
import ru.relabs.kurjer.utils.SearchUtils
import java.io.File

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

data class AddressesState(
    val loaders: Int = 0,
    val tasks: List<Task> = emptyList(),
    val sorting: AddressesSortingMethod = AddressesSortingMethod.STANDARD,
    val searchFilter: String = "",
    val exits: Int = 0,
    val selectedListAddress: Address? = null
) {
    val sortedTasks: List<AddressesItem>
        get() = getSortedTasks(tasks, sorting, searchFilter)

    val otherCount: Int
        get() {
            val allTaskItemsCount = tasks.sumOf { it.items.size }
            val filteredTaskItemsCount = if (searchFilter.isNotEmpty()) {
                tasks.sumOf { it.items.filter { ti -> SearchUtils.isMatches(ti.address.name, searchFilter) }.size }
            } else {
                allTaskItemsCount
            }
            return allTaskItemsCount - filteredTaskItemsCount
        }
    private fun getSortedTasks(tasks: List<Task>, sorting: AddressesSortingMethod, searchFilter: String): List<AddressesItem> {
        if (tasks.isEmpty()) {
            return emptyList()
        }

        val taskItems = tasks.flatMap { task -> task.items.map { item -> task to item } }.let { items ->
            if (searchFilter.isNotEmpty()) {
                items.filter { (_, item) ->
                    when (item) {
                        is TaskItem.Common -> false
                        is TaskItem.Firm -> SearchUtils.isMatches("${item.firmName}, ${item.office}", searchFilter)
                    } || SearchUtils.isMatches(item.address.name, searchFilter)
                }
            } else {
                items
            }
        }

        if (taskItems.isEmpty()) {
            return emptyList()
        }

        val sortedItems = when (sorting) {
            AddressesSortingMethod.STANDARD -> taskItems.sortedWith(compareBy<Pair<Task, TaskItem>> { it.second.subarea }
                .thenBy { it.second.bypass }
                .thenBy { it.second.address.city }
                .thenBy { it.second.address.street }
                .thenBy { it.second.address.house }
                .thenBy { it.second.address.houseName }
                .thenBy { it.second.state }
            ).groupBy {
                it.second.address.id
            }.toList().sortedBy {
                !it.second.any { it.second.state != TaskItemState.CLOSED }
            }.toMap().flatMap {
                it.value
            }
            AddressesSortingMethod.ALPHABETIC -> taskItems.sortedWith(compareBy<Pair<Task, TaskItem>> { it.second.address.city }
                .thenBy { it.second.address.street }
                .thenBy { it.second.address.house }
                .thenBy { it.second.address.houseName }
                .thenBy { it.second.state }
            ).groupBy {
                it.second.address.id
            }.toList().sortedBy {
                !it.second.any { it.second.state != TaskItemState.CLOSED }
            }.toMap().flatMap {
                it.value
            }
        }

        val groups = sortedItems
            .groupBy { it.second.address.id }
            .map {
                listOf(AddressesItem.GroupHeader(it.value.map { it.second }, tasks.size == 1)) + it.value.map { (t, ti) ->
                    when (ti) {
                        is TaskItem.Common -> AddressesItem.AddressItem(ti, t)
                        is TaskItem.Firm -> AddressesItem.FirmItem(ti, t)
                    }
                }
            }
            .flatten()

        return groups
    }
}

class AddressesContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {

    val taskRepository: TaskRepository by inject()
    val taskEventController: TaskEventController by inject()
    val pathsProvider: PathsProvider by inject()

    var showImagePreview: (File) -> Unit = {}
    var showSnackbar: (msgRes: Int) -> Unit = {}
}

typealias AddressesMessage = ElmMessage<AddressesContext, AddressesState>
typealias AddressesEffect = ElmEffect<AddressesContext, AddressesState>
typealias AddressesRender = ElmRender<AddressesState>

enum class AddressesSortingMethod {
    STANDARD, ALPHABETIC
}
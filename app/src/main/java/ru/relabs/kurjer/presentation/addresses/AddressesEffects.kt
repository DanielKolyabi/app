package ru.relabs.kurjer.presentation.addresses

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.domain.controllers.TaskEvent
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.presentation.RootScreen

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */
object AddressesEffects {

    fun effectLoadTasks(taskIds: List<TaskId>): AddressesEffect = { c, s ->
        messages.send(AddressesMessages.msgAddLoaders(1))
        val tasks = taskIds.mapNotNull {
            //TODO: Log if null
            c.databaseRepository.getTask(it)
        }
        messages.send(AddressesMessages.msgTasksLoaded(tasks))
        messages.send(AddressesMessages.msgAddLoaders(-1))
    }

    fun effectNavigateBack(): AddressesEffect = { c, s ->
        withContext(Dispatchers.Main) {
            c.router.exit()
        }
    }

    fun effectNavigateReport(task: Task, item: TaskItem): AddressesEffect = { c, s ->
        val sameAddressItems = s.tasks
            .flatMap { it.items.map { taskItem -> it to taskItem } }
            .filter { it.second.address.id == item.address.id }

        withContext(Dispatchers.Main) {
            c.router.navigateTo(RootScreen.Report(sameAddressItems, item))
        }
    }

    fun effectLaunchEventConsumer(): AddressesEffect = { c, s ->
        coroutineScope {
            launch {
                c.taskEventController.subscribe().collect {
                    when(it){
                        is TaskEvent.TaskClosed ->
                            messages.send(AddressesMessages.msgTaskClosed(it.taskId))
                        is TaskEvent.TaskItemClosed ->
                            messages.send(AddressesMessages.msgTaskItemClosed(it.taskItemId))
                    }
                }
            }
        }
    }
}
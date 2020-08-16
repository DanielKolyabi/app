package ru.relabs.kurjer.presentation.addresses

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import ru.relabs.kurjer.R
import ru.relabs.kurjer.ReportService
import ru.relabs.kurjer.domain.controllers.TaskEvent
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.files.PathHelper
import ru.relabs.kurjer.presentation.RootScreen
import ru.relabs.kurjer.presentation.base.tea.msgEffect

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

    fun effectNavigateBack(stopTimer: Boolean = false): AddressesEffect = { c, s ->
        if (!s.isExited) {
            if (stopTimer) {
                ReportService.instance?.stopTimer()
            }
            withContext(Dispatchers.Main) {
                c.router.exit()
            }
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
                    when (it) {
                        is TaskEvent.TaskClosed ->
                            messages.send(AddressesMessages.msgRemoveTask(it.taskId))
                        is TaskEvent.TaskItemClosed ->
                            messages.send(AddressesMessages.msgTaskItemClosed(it.taskItemId))
                    }
                }
            }
        }
    }

    fun effectValidateTasks(): AddressesEffect = { c, s ->
        messages.send(AddressesMessages.msgAddLoaders(1))
        s.tasks.forEach { t ->
            val updatedTask = c.databaseRepository.getTask(t.id) ?: return@forEach
            if (updatedTask.items.none { it.state == TaskItemState.CREATED }) {
                c.databaseRepository.closeTaskById(updatedTask.id, true)
            }
            if (!(updatedTask.state.state == TaskState.EXAMINED || updatedTask.state.state == TaskState.STARTED)) {
                messages.send(AddressesMessages.msgRemoveTask(updatedTask.id))
            }
        }
        messages.send(AddressesMessages.msgAddLoaders(-1))
    }

    fun effectOpenImageMap(task: Task): AddressesEffect = { c, s ->
        messages.send(AddressesMessages.msgAddLoaders(1))
        val file = PathHelper.getTaskRasterizeMapFile(task)
        withContext(Dispatchers.Main) {
            when (file.exists()) {
                true -> c.showImagePreview(file)
                else -> c.showSnackbar(R.string.image_map_not_found)
            }
        }
        messages.send(AddressesMessages.msgAddLoaders(-1))
    }

    fun effectOpenYandexMap(taskItems: List<TaskItem>): AddressesEffect = { c, s ->
        withContext(Dispatchers.Main) {
            c.router.navigateTo(
                RootScreen.YandexMap(taskItems) {
                    messages.offer(msgEffect(effectYandexMapAddressSelected(it)))
                }
            )
        }
    }

    private fun effectYandexMapAddressSelected(address: Address): AddressesEffect = { c, s ->
        messages.send(AddressesMessages.msgSelectedListAddress(address))
        delay(1000)
        messages.send(AddressesMessages.msgSelectedListAddress(null))
    }
}
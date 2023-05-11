package ru.relabs.kurjer.presentation.tasks

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.controllers.TaskEvent
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskState
import ru.relabs.kurjer.domain.repositories.MergeResult
import ru.relabs.kurjer.presentation.RootScreen
import ru.relabs.kurjer.presentation.base.tea.CommonMessages
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.utils.Left
import ru.relabs.kurjer.utils.Right
import java.util.*

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */
object TasksEffects {

    fun effectNavigateTaskInfo(task: Task): TasksEffect = { c, s ->
        withContext(Dispatchers.Main) {
            c.router.navigateTo(RootScreen.taskInfo(task, c.examinedConsumer))
        }
    }

    fun effectTaskSelected(task: Task): TasksEffect = { c, s ->
        when {
            task.state.state != TaskState.EXAMINED && task.state.state != TaskState.STARTED ->
                c.showSnackbar(R.string.task_list_not_examined)
            Date() < task.startTime ->
                c.showSnackbar(R.string.task_list_not_started)
            else ->
                messages.send(TasksMessages.msgTaskSelected(task))
        }
    }

    fun effectTaskUnselected(task: Task): TasksEffect = { c, s ->
        messages.send(TasksMessages.msgTaskUnselected(task))
    }

    fun effectNavigateAddresses(): TasksEffect = { c, s ->
        val photos = s.selectedTasks
            .map { c.pathsProvider.getEditionPhotoFile(it) }
            .filter { it.exists() }
            .map { it.path }
        withContext(Dispatchers.Main) {
            if (photos.isNotEmpty()) {
                c.router.newChain(
                    RootScreen.addresses(s.selectedTasks),
                    RootScreen.imagePreview(photos)
                )
            } else {
                c.router.navigateTo(RootScreen.addresses(s.selectedTasks))
            }
        }
    }

    fun effectLoadTasks(withNetwork: Boolean): TasksEffect = { c, s ->
        messages.send(TasksMessages.msgAddLoaders(1))
        if (withNetwork) {
            var tasksUpdated = false
            var tasksCreated = false

            when (val r = c.deliveryRepository.getTasks()) {
                is Right -> c.taskRepository.mergeTasks(r.value).collect {
                    when (it) {
                        is MergeResult.TaskCreated -> {
                            c.deliveryRepository.loadTaskMap(it.task)
                            c.deliveryRepository.loadEditionPhoto(it.task)
                            tasksCreated = true
                        }
                        is MergeResult.TaskUpdated -> {
                            c.deliveryRepository.loadTaskMap(it.task)
                            c.deliveryRepository.loadEditionPhoto(it.task)
                            tasksUpdated = true
                        }
                        is MergeResult.TaskRemoved -> tasksUpdated = true
                    }
                }
                is Left -> messages.send(CommonMessages.msgError(r.value))
            }

            val message = when {
                tasksUpdated -> R.string.task_list_tasks_updated
                tasksCreated -> R.string.task_list_tasks_created
                else -> R.string.task_list_tasks_not_changed
            }
            c.showSnackbar(message)
            c.deliveryRepository.getFirmRejectReasons(true)
        }
        messages.send(TasksMessages.msgTasksLoaded(c.taskRepository.getTasks()))
        messages.send(TasksMessages.msgAddLoaders(-1))
    }

    fun effectRefresh(): TasksEffect = { c, s ->
        when (s.loaders > 0) {
            true -> c.showSnackbar(R.string.task_list_update_in_progress)
            false -> messages.send(msgEffect(effectLoadTasks(true)))
        }
    }

    fun effectLaunchEventConsumers(): TasksEffect = { c, s ->
        coroutineScope {
            launch {
                c.taskEventController.subscribe().collect { event ->
                    when (event) {
                        is TaskEvent.TaskClosed ->
                            messages.send(TasksMessages.msgTaskClosed(event.taskId))
                        is TaskEvent.TasksUpdateRequired -> withContext(Dispatchers.Main) {
                            if (event.showDialogInTasks && s.loaders == 0) {
                                c.showUpdateRequiredOnVisible(c.settingsRepository.canSkipUpdates)
                            }
                        }
                        is TaskEvent.TaskItemClosed -> Unit
                        is TaskEvent.TaskStorageClosed -> Unit
                    }
                }
            }
        }
    }

    fun effectShowTaskSelectionDistrictError(): TasksEffect = { c, _ ->
        c.showSnackbar(R.string.task_list_district_different)
    }
}
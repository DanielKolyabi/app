package ru.relabs.kurjer.presentation.tasks

import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.base.tea.msgEffects
import ru.relabs.kurjer.presentation.base.tea.msgState

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

object TasksMessages {
    fun msgInit(refreshTasks: Boolean): TasksMessage = msgEffects(
        { it },
        { listOf(TasksEffects.effectLoadTasks(refreshTasks)) }
    )

    fun msgTaskSelectClick(task: Task): TasksMessage = msgEffects(
        { it },
        {
            listOf(
                if (it.selectedTasks.contains(task)) {
                    TasksEffects.effectTaskUnselected(task)
                } else {
                    TasksEffects.effectTaskSelected(task)
                }
            )
        }
    )

    fun msgTaskClicked(task: Task): TasksMessage =
        msgEffect(TasksEffects.effectNavigateTaskInfo(task))

    fun msgTaskSelected(task: Task): TasksMessage =
        msgState { it.copy(selectedTasks = it.selectedTasks + listOf(task)) }

    fun msgTaskUnselected(task: Task): TasksMessage =
        msgState { it.copy(selectedTasks = it.selectedTasks.filter { it != task }) }

    fun msgStartClicked(): TasksMessage =
        msgEffect(TasksEffects.effectNavigateAddresses())

    fun msgAddLoaders(i: Int): TasksMessage =
        msgState { it.copy(loaders = it.loaders + i) }

    fun msgTasksLoaded(tasks: List<Task>): TasksMessage =
        msgState { it.copy(tasks = tasks) }

    fun msgRefresh(): TasksMessage =
        msgEffect(TasksEffects.effectRefresh())
}
package ru.relabs.kurjer.presentation.taskDetails

import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.presentation.base.tea.msgEffects
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.base.tea.msgState

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

object TaskDetailsMessages {
    fun msgInit(task: Task?): TaskDetailsMessage = msgEffects(
        { it.copy(task = task) },
        { listOf() }
    )

    fun msgAddLoaders(i: Int): TaskDetailsMessage =
        msgState { it.copy(loaders = it.loaders + i) }

    fun msgNavigateBack(): TaskDetailsMessage =
        msgEffect(TaskDetailsEffects.effectNavigateBack())

    fun msgInfoClicked(taskItem: TaskItem): TaskDetailsMessage =
        msgEffect(TaskDetailsEffects.effectNavigateTaskItemDetails(taskItem))

    fun msgExamineClicked(): TaskDetailsMessage =
        msgEffect(TaskDetailsEffects.effectExamine())

    fun msgOpenMap(): TaskDetailsMessage =
        msgEffect(TaskDetailsEffects.effectOpenMap())
}
package ru.relabs.kurjer.presentation.tasks

import android.view.View
import androidx.recyclerview.widget.DiffUtil
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.presentation.base.DefaultListDiffCallback
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.renderT
import ru.relabs.kurjer.utils.extensions.visible

/**
 * Created by Daniil Kurchanov on 06.04.2020.
 */
object TasksRenders {
    fun renderList(adapter: DelegateAdapter<TasksItem>): TasksRender = renderT(
        { Triple(it.tasks, it.selectedTasks, it.loaders) },
        { (tasks, selectedTasks, loaders) ->
            val intersections = searchIntersections(tasks, selectedTasks)
            val newItems = if (tasks.isEmpty() && loaders > 0) {
                listOf(TasksItem.Loader)
            } else {
                tasks.map {
                    TasksItem.TaskItem(it, intersections.getOrElse(it) { false }, selectedTasks.contains(it))
                }
            }

            val diff = DiffUtil.calculateDiff(DefaultListDiffCallback(adapter.items, newItems))

            adapter.items.clear()
            adapter.items.addAll(newItems)
            diff.dispatchUpdatesTo(adapter)
        }
    )

    fun renderLoading(view: View): TasksRender = renderT(
        { it.tasks to (it.loaders > 0) },
        { (tasks, loading) ->
            view.visible = tasks.isNotEmpty() && loading
        }
    )

    fun renderStartButton(view: View): TasksRender = renderT(
        { it.selectedTasks.isNotEmpty() },
        { view.visible = it }
    )

    private fun searchIntersections(
        tasks: List<Task>,
        selectedTasks: List<Task>
    ): Map<Task, Boolean> {
        //TODO: May be optimize :/
        val result = mutableMapOf<Task, Boolean>()
        tasks.forEach { task ->
            selectedTasks.forEach { selectedTask ->
                val isSelectedTaskContainsTaskAddress = task.items.any { taskItem ->
                    selectedTask.items.any { selectedTaskItem -> selectedTaskItem.address.id == taskItem.address.id }
                }

                if (isSelectedTaskContainsTaskAddress) {
                    result[task] = true
                }
            }
        }
        return result
    }
}
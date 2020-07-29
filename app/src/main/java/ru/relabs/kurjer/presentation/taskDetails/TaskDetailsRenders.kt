package ru.relabs.kurjer.presentation.taskDetails

import android.view.View
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.TaskItemState
import ru.relabs.kurjer.domain.models.TaskState
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.renderT
import ru.relabs.kurjer.uiOld.models.DetailsListModel
import ru.relabs.kurjer.utils.extensions.visible

/**
 * Created by Daniil Kurchanov on 06.04.2020.
 */
object TaskDetailsRenders {
    fun renderLoading(view: View): TaskDetailsRender = renderT(
        { it.loaders > 0 },
        { view.visible = it }
    )

    fun renderList(adapter: DelegateAdapter<TaskDetailsItem>): TaskDetailsRender = renderT(
        { it.task },
        {task ->
            adapter.items.clear()
            if(task != null){
                adapter.items.add(TaskDetailsItem.PageHeader(task))
                adapter.items.add(TaskDetailsItem.ListHeader)
                adapter.items.addAll(
                    task.items.sortedWith(compareBy<TaskItem> { it.subarea }
                        .thenBy { it.bypass }
                        .thenBy { it.address.city }
                        .thenBy { it.address.street }
                        .thenBy { it.address.house }
                        .thenBy { it.address.houseName }
                        .thenBy { it.state }
                    ).groupBy {
                        it.address.id
                    }.toList().sortedBy {
                        !it.second.any { it.state != TaskItemState.CLOSED }
                    }.toMap().flatMap {
                        it.value
                    }.map { TaskDetailsItem.ListItem(it) }
                )
            }
            adapter.notifyDataSetChanged()
        }
    )

    fun renderExamine(view: View): TaskDetailsRender = renderT(
        { it.task?.state?.state == TaskState.CREATED },
        { view.visible = it }
    )
}
package ru.relabs.kurjer.ui.holders

import android.view.View
import kotlinx.android.synthetic.main.item_task_list_task.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.ui.helpers.setVisible
import ru.relabs.kurjer.ui.models.TaskListModel

/**
 * Created by ProOrange on 31.08.2018.
 */
class TaskListTaskHolder(
        val view: View,
        val onSelectedClicked: (position: Int) -> Unit,
        val onTaskClicked: (position: Int) -> Unit
) : BaseViewHolder<TaskListModel>(view) {
    override fun onBindViewHolder(item: TaskListModel) {
        if(item !is TaskListModel.Task) return

        view.title.text = "${item.task.name} №${item.task.edition}, ${item.task.copies}экз., (${item.task.brigade}бр/${item.task.area}уч)"
        setIsSelected(item.task.state > 0)
        setIsActive(item.task.selected)

        view.selected_icon.setOnClickListener {
            onSelectedClicked(this.adapterPosition)
        }

        view.container.setOnClickListener {
            onTaskClicked(this.adapterPosition)
        }
    }

    fun setIsSelected(selected: Boolean) {
        view.active_icon.setVisible(selected)
    }

    fun setIsActive(active: Boolean) {
        view.selected_icon.setImageDrawable(view.resources.getDrawable(
                if (active)
                    R.drawable.ic_chain_enabled
                else
                    R.drawable.ic_chain_disabled
        ))
    }
}

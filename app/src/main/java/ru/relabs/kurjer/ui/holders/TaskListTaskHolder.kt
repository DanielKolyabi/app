package ru.relabs.kurjer.ui.holders

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.View
import kotlinx.android.synthetic.main.item_task_list_task.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.TaskModel
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

        var realState = item.task.state
        if(item.task.state and TaskModel.BY_OTHER_USER != 0){
            realState = item.task.state xor TaskModel.BY_OTHER_USER
        }

        setIsSelected(realState > 0, item.task.state and TaskModel.BY_OTHER_USER != 0)
        setIsActive(item.task.selected)

        view.selected_icon.setOnClickListener {
            onSelectedClicked(this.adapterPosition)
        }

        view.container.setOnClickListener {
            onTaskClicked(this.adapterPosition)
        }

        if(item.hasSelectedTasksWithSimilarAddress){
            view.setBackgroundColor(Color.GRAY)
        }else{
            view.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    fun setIsSelected(selected: Boolean, byOtherUser: Boolean) {
        view.active_icon.setVisible(selected)
        if(byOtherUser){
            view.active_icon.setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN)
        }else{
            view.active_icon.setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY)
        }
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

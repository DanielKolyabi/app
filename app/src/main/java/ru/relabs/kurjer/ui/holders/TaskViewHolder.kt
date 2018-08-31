package ru.relabs.kurjer.ui.holders

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_task_list_task.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.helpers.setVisible

class TaskViewHolder(
        val view: View,
        val onSelectedClicked: (position: Int) -> Unit,
        val onTaskClicked: (position: Int) -> Unit
) : RecyclerView.ViewHolder(view) {

    @SuppressLint("SetTextI18n")
    fun bind(data: TaskModel) {
        view.title.text = "${data.name} №${data.edition}, ${data.copies}экз., (${data.brigade}бр/${data.area}уч)"
        setIsSelected(data.state > 0)
        setIsActive(data.selected)

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

package ru.relabs.kurjer.ui.holders

import android.view.View
import kotlinx.android.synthetic.main.item_addr_list_task.view.*
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.ui.models.AddressListModel

/**
 * Created by ProOrange on 11.08.2018.
 */
class AddressListTaskItemHolder(
        itemView: View,
        val onItemClicked: (item: AddressListModel.TaskItem) -> Unit,
        private val onItemMapClicked: (task: TaskModel) -> Unit) : BaseViewHolder<AddressListModel>(itemView) {

    override fun onBindViewHolder(item: AddressListModel) {
        if (item !is AddressListModel.TaskItem) return
        itemView.task_button.text = "${item.parentTask.name} №${item.parentTask.edition}, ${item.taskItem.copies}экз."
        if (item.taskItem.state == TaskItemModel.CLOSED) {
            //itemView.task_button.isEnabled = false
            itemView.map_icon.alpha = 0.4f
            itemView.map_icon.isClickable = false
        } else {
            //itemView.task_button.isEnabled = true
            itemView.map_icon.alpha = 1f
            itemView.map_icon.isClickable = true
        }
        itemView.task_button.setOnClickListener {
            onItemClicked(item)
        }
        itemView.map_icon.setOnClickListener {
            onItemMapClicked(item.parentTask)
        }
    }
}
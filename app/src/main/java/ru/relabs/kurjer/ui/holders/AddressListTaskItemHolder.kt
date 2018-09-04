package ru.relabs.kurjer.ui.holders

import android.view.View
import kotlinx.android.synthetic.main.item_addr_list_task.view.*
import ru.relabs.kurjer.ui.models.AddressListModel
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder

/**
 * Created by ProOrange on 11.08.2018.
 */
class AddressListTaskItemHolder(itemView: View, val onItemClicked: (addressId: Int) -> Unit) : BaseViewHolder<AddressListModel>(itemView) {
    override fun onBindViewHolder(item: AddressListModel) {
        if (item !is AddressListModel.TaskItem) return
        itemView.task_button.text = "${item.parentTask.name} №${item.parentTask.edition}, ${item.taskItem.copies}экз."
        if (item.taskItem.state == TaskItemModel.CLOSED) {
            itemView.task_button.isEnabled = false
            itemView.map_icon.alpha = 0.4f
            itemView.map_icon.isClickable = false
        }
        itemView.task_button.setOnClickListener {
            onItemClicked(item.taskItem.address.id)
        }
    }
}
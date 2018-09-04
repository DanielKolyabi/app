package ru.relabs.kurjer.ui.holders

import android.view.View
import kotlinx.android.synthetic.main.item_task_details_list_item.view.*
import ru.relabs.kurjer.ui.models.DetailsListModel
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder

/**
 * Created by ProOrange on 29.08.2018.
 */
class DetailsTableItemHolder(itemView: View, val onInfoClicked: (item: TaskItemModel) -> Unit) : BaseViewHolder<DetailsListModel>(itemView) {
    override fun onBindViewHolder(item: DetailsListModel) {
        if (item !is DetailsListModel.TaskItem) return
        val taskItem = item.taskItem
        with(itemView){
            number_text.text = "${taskItem.subarea}    ${taskItem.bypass}"
            address_text.text = taskItem.address.name
            copies_text.text = taskItem.copies.toString()

            info_icon.setOnClickListener {
                onInfoClicked(taskItem)
            }
        }
    }
}
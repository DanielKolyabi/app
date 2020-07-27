package ru.relabs.kurjer.uiOld.holders

import android.graphics.Color
import android.view.View
import kotlinx.android.synthetic.main.item_task_details_list_item.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.uiOld.models.DetailsListModel
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.uiOld.delegateAdapter.BaseViewHolder

/**
 * Created by ProOrange on 29.08.2018.
 */
class DetailsTableItemHolder(itemView: View, val onInfoClicked: (item: TaskItem) -> Unit) : BaseViewHolder<DetailsListModel>(itemView) {
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

            if(taskItem.needPhoto){
                address_text.setTextColor(resources.getColor(R.color.colorFuchsia))
            }else{
                address_text.setTextColor(Color.parseColor("#808080"))
            }
        }
    }
}
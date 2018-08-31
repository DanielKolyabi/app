package ru.relabs.kurjer.ui.holders

import android.view.View
import kotlinx.android.synthetic.main.item_task_details_list_info.view.*
import ru.relabs.kurjer.models.DetailsListModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.ui.helpers.formated

/**
 * Created by ProOrange on 29.08.2018.
 */
class DetailsTableInfoHolder(itemView: View) : BaseViewHolder<DetailsListModel>(itemView) {
    override fun onBindViewHolder(item: DetailsListModel) {
        if (item !is DetailsListModel.Task) return
        val task = item.task
        with(itemView) {
            publisher_text.text = "${task.name} №${task.edition}"
            dates_text.text = task.startTime.formated() + " - " + task.endTime.formated()
            brigade_text.text = task.brigade.toString()
            area_text.text = task.area.toString()
            city_text.text = task.city
            copies_text.text = task.copies.toString()
            pack_count_text.text = task.packs.toString()
            remain_text.text = task.remain.toString()
            storage_text.text = task.storageAddress
        }
    }
}
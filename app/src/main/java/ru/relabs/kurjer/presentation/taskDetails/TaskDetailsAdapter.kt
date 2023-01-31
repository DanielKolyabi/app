package ru.relabs.kurjer.presentation.taskDetails

import android.graphics.Color
import android.view.View
import kotlinx.android.synthetic.main.holder_task_details_list_info.view.*
import kotlinx.android.synthetic.main.holder_task_details_list_info.view.tv_copies
import kotlinx.android.synthetic.main.holder_task_details_list_item.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.presentation.base.recycler.IAdapterDelegate
import ru.relabs.kurjer.presentation.base.recycler.delegateDefine
import ru.relabs.kurjer.presentation.base.recycler.holderDefine
import ru.relabs.kurjer.uiOld.helpers.formated

object TaskDetailsAdapter {
    fun pageHeaderAdapter(): IAdapterDelegate<TaskDetailsItem> = delegateDefine(
        { it is TaskDetailsItem.PageHeader },
        { p ->
            holderDefine(p, R.layout.holder_task_details_list_info, { it as TaskDetailsItem.PageHeader }) { (task) ->
                with(itemView) {
                    tv_publisher.text = resources.getString(R.string.task_details_publisher, task.name, task.edition)
                    tv_dates.text =
                        resources.getString(R.string.task_details_date_rande, task.startTime.formated(), task.endTime.formated())
                    tv_brigade.text = task.brigade.toString()
                    tv_area.text = task.area.toString()
                    tv_city.text = task.city
                    tv_copies.text = task.copies.toString()
                    tv_pack_count.text = task.packs.toString()
                    tv_remain.text = task.remain.toString()
                    tv_storage.text = task.storage.address

                    if (task.items.any { it.needPhoto || (it is TaskItem.Common && it.entrancesData.any { it.photoRequired }) }) {
                        need_photo_label.visibility = View.VISIBLE
                    } else {
                        need_photo_label.visibility = View.GONE
                    }
                }
            }
        }
    )

    fun listHeaderAdapter(): IAdapterDelegate<TaskDetailsItem> = delegateDefine(
        { it is TaskDetailsItem.ListHeader },
        { p ->
            holderDefine(p, R.layout.holder_task_details_list_header) { it as TaskDetailsItem.ListHeader }
        }
    )

    fun listItemAdapter(
        onInfoClicked: (TaskItem) -> Unit
    ): IAdapterDelegate<TaskDetailsItem> = delegateDefine(
        { it is TaskDetailsItem.ListItem },
        { p ->
            holderDefine(p, R.layout.holder_task_details_list_item, { it as TaskDetailsItem.ListItem }) { (taskItem) ->
                with(itemView) {
                    tv_bypass.text = resources.getString(R.string.task_details_address_bypass, taskItem.subarea, taskItem.bypass)
                    tv_address.text = when (taskItem) {
                        is TaskItem.Common -> taskItem.address.name
                        is TaskItem.Firm -> listOf(taskItem.address.name, taskItem.firmName, taskItem.office)
                            .filter { it.isNotEmpty() }
                            .joinToString(", ")
                    }
                    tv_copies.text = taskItem.copies.toString()

                    iv_info.setOnClickListener {
                        onInfoClicked(taskItem)
                    }

                    if (taskItem.needPhoto || (taskItem is TaskItem.Common && taskItem.entrancesData.any { it.photoRequired })) {
                        tv_address.setTextColor(resources.getColor(R.color.colorFuchsia))
                    } else {
                        tv_address.setTextColor(Color.parseColor("#808080"))
                    }
                }
            }
        }
    )
}
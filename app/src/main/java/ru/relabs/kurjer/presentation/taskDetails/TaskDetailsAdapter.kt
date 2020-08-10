package ru.relabs.kurjer.presentation.taskDetails

import android.graphics.Color
import android.view.View
import kotlinx.android.synthetic.main.holder_task_details_list_info.view.*
import kotlinx.android.synthetic.main.holder_task_details_list_info.view.tv_copies
import kotlinx.android.synthetic.main.holder_task_details_list_item.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.TaskItem
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
                    tv_storage.text = task.storageAddress

                    if (task.items.any { it.needPhoto || it.entrancesData.any { it.photoRequired } }) {
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
                    tv_address.text = taskItem.address.name
                    tv_copies.text = taskItem.copies.toString()

                    iv_info.setOnClickListener {
                        onInfoClicked(taskItem)
                    }

                    if (taskItem.needPhoto || taskItem.entrancesData.any { it.photoRequired }) {
                        tv_address.setTextColor(resources.getColor(R.color.colorFuchsia))
                    } else {
                        tv_address.setTextColor(Color.parseColor("#808080"))
                    }
                }
            }
        }
    )
}
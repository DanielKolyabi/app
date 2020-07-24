package ru.relabs.kurjer.uiOld.holders

import android.view.View
import kotlinx.android.synthetic.main.item_report_task.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.uiOld.models.ReportTasksListModel
import ru.relabs.kurjer.uiOld.delegateAdapter.BaseViewHolder

class ReportTaskHolder(itemView: View, val onTaskClicked: (pos: Int) -> Unit) : BaseViewHolder<ReportTasksListModel>(itemView) {
    override fun onBindViewHolder(item: ReportTasksListModel) {
        if(item !is ReportTasksListModel.TaskButton) return
        itemView.button.text = "${item.task.name} №${item.task.edition}, ${item.task.copies}"
        if(item.active){
            itemView.button.setBackgroundResource(R.drawable.abc_btn_colored_material)
        }else{
            itemView.button.setBackgroundResource(R.drawable.abc_btn_default_mtrl_shape)
        }

        itemView.button.setOnClickListener {
            onTaskClicked(item.pos)
        }
    }
}

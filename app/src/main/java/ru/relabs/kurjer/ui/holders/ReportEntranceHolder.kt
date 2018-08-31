package ru.relabs.kurjer.ui.holders

import android.view.View
import kotlinx.android.synthetic.main.item_report_entrance.view.*
import ru.relabs.kurjer.models.ReportEntrancesListModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder

class ReportEntranceHolder(itemView: View) : BaseViewHolder<ReportEntrancesListModel>(itemView) {
    override fun onBindViewHolder(item: ReportEntrancesListModel) {
        if(item !is ReportEntrancesListModel.Entrance) return
        itemView.entrance_title.text = "Подъезд ${item.entranceNumber}"
    }
}

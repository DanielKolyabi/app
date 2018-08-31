package ru.relabs.kurjer.ui.holders

import android.view.View
import kotlinx.android.synthetic.main.item_report_entrance.view.*
import ru.relabs.kurjer.models.ReportEntrancesListModel
import ru.relabs.kurjer.models.ReportPhotosListModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder

/**
 * Created by ProOrange on 30.08.2018.
 */
class ReportBlankPhotoHolder(itemView: View) : BaseViewHolder<ReportPhotosListModel>(itemView) {
    override fun onBindViewHolder(item: ReportPhotosListModel) {
        if(item !is ReportPhotosListModel.BlankPhoto) return
    }
}

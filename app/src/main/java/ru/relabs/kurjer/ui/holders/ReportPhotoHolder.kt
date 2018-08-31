package ru.relabs.kurjer.ui.holders

import android.view.View
import ru.relabs.kurjer.models.ReportPhotosListModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder

/**
 * Created by ProOrange on 30.08.2018.
 */

class ReportPhotoHolder(itemView: View) : BaseViewHolder<ReportPhotosListModel>(itemView) {
    override fun onBindViewHolder(item: ReportPhotosListModel) {
        if(item !is ReportPhotosListModel.TaskItemPhoto) return
    }
}

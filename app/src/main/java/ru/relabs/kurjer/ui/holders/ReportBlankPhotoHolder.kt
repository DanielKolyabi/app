package ru.relabs.kurjer.ui.holders

import android.support.v7.widget.RecyclerView
import android.view.View
import ru.relabs.kurjer.ui.models.ReportPhotosListModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder

/**
 * Created by ProOrange on 30.08.2018.
 */
class ReportBlankPhotoHolder(itemView: View, private val onPhotoClicked: (holder: RecyclerView.ViewHolder) -> Unit) : BaseViewHolder<ReportPhotosListModel>(itemView) {
    override fun onBindViewHolder(item: ReportPhotosListModel) {
        if(item !is ReportPhotosListModel.BlankPhoto) return
        itemView.setOnClickListener {
            onPhotoClicked(this)
        }
    }
}

package ru.relabs.kurjer.uiOld.holders

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import ru.relabs.kurjer.uiOld.models.ReportPhotosListModel
import ru.relabs.kurjer.uiOld.delegateAdapter.BaseViewHolder

/**
 * Created by ProOrange on 30.08.2018.
 */
class ReportBlankMultiPhotoHolder(itemView: View, private val onPhotoClicked: (holder: RecyclerView.ViewHolder) -> Unit) : BaseViewHolder<ReportPhotosListModel>(itemView) {
    override fun onBindViewHolder(item: ReportPhotosListModel) {
        if(item !is ReportPhotosListModel.BlankMultiPhoto) return
        itemView.setOnClickListener {
            onPhotoClicked(this)
        }
    }
}
package ru.relabs.kurjer.uiOld.delegates

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import ru.relabs.kurjer.R
import ru.relabs.kurjer.uiOld.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.uiOld.delegateAdapter.IAdapterDelegate
import ru.relabs.kurjer.uiOld.holders.ReportBlankMultiPhotoHolder
import ru.relabs.kurjer.uiOld.models.ReportPhotosListModel

/**
 * Created by ProOrange on 28.11.2018.
 */

class ReportBlankMultiPhotoDelegate(private val onPhotoClicked: (holder: RecyclerView.ViewHolder) -> Unit) : IAdapterDelegate<ReportPhotosListModel> {
    override fun isForViewType(data: List<ReportPhotosListModel>, position: Int): Boolean {
        return data[position] is ReportPhotosListModel.BlankMultiPhoto
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ReportPhotosListModel>, data: List<ReportPhotosListModel>, position: Int) {
        holder.onBindViewHolder(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ReportPhotosListModel> {
        return ReportBlankMultiPhotoHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_report_photo_blank_multi, parent, false),
                onPhotoClicked
        )
    }
}


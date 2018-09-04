package ru.relabs.kurjer.ui.delegates

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import ru.relabs.kurjer.R
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.ui.delegateAdapter.IAdapterDelegate
import ru.relabs.kurjer.ui.holders.ReportPhotoHolder
import ru.relabs.kurjer.ui.models.ReportPhotosListModel

/**
 * Created by ProOrange on 30.08.2018.
 */
class ReportPhotoDelegate(private val onRemoveClicked: (holder: RecyclerView.ViewHolder) -> Unit) : IAdapterDelegate<ReportPhotosListModel> {
    override fun isForViewType(data: List<ReportPhotosListModel>, position: Int): Boolean {
        return data[position] is ReportPhotosListModel.TaskItemPhoto
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ReportPhotosListModel>, data: List<ReportPhotosListModel>, position: Int) {
        holder.onBindViewHolder(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ReportPhotosListModel> {
        return ReportPhotoHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_report_photo, parent, false), onRemoveClicked)
    }
}


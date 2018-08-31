package ru.relabs.kurjer.ui.delegates

import android.view.LayoutInflater
import android.view.ViewGroup
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.ReportPhotosListModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.ui.delegateAdapter.IAdapterDelegate
import ru.relabs.kurjer.ui.holders.ReportBlankPhotoHolder

/**
 * Created by ProOrange on 30.08.2018.
 */
class ReportPhotoDelegate : IAdapterDelegate<ReportPhotosListModel> {
    override fun isForViewType(data: List<ReportPhotosListModel>, position: Int): Boolean {
        return data[position] is ReportPhotosListModel.TaskItemPhoto
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ReportPhotosListModel>, data: List<ReportPhotosListModel>, position: Int) {
        holder.onBindViewHolder(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ReportPhotosListModel> {
        return ReportBlankPhotoHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_report_photo, parent, false))
    }
}

